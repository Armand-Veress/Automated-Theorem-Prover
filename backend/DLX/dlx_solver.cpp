//
// Created by arman on 5/31/202

#include <cstdio>
#include <chrono>
#include <iostream>
#include "dlx_solver.h"

static long long debug_iterations = 0;
static std::chrono::steady_clock::time_point start_time;
static bool kill_flag = false;

dlx_solver::dlx_solver(const int *flat_matrix) {
    root = new Node();
    root->left = root;
    root->right = root;
    root->up = root;
    root->down = root;
    root->column_header = root;
    buildGraph(flat_matrix);
}

dlx_solver::~dlx_solver() {
    cleanup();
}

void dlx_solver::cleanup() {
    Node* current_column = root->right;
    while (current_column != root) {
        Node* current_node = current_column->down;
        while (current_node != current_column) {
            Node* aux = current_node;
            current_node = current_node->down;
            delete aux;
        }
        Node* aux = current_column;
        current_column = current_column->right;
        delete aux;
    }
    delete root;
}

void dlx_solver::cover(Node* column_header) {
    column_header->right->left = column_header->left;
    column_header->left->right = column_header->right;

    for (Node* row = column_header->down; row != column_header; row = row->down) {
        for (Node* node = row->right; node != row; node = node->right) {
            node->down->up = node->up;
            node->up->down = node->down;
            node->column_header->size--;
        }
    }
}

void dlx_solver::uncover(Node* column_header) {
    for (Node* row = column_header->up; row != column_header; row = row->up) {
        for (Node* node = row->left; node != row; node = node->left) {
            node->column_header->size++;
            node->up->down = node;
            node->down->up = node;
        }
    }
    column_header->left->right = column_header;
    column_header->right->left = column_header;
}

void dlx_solver::solve() {
    debug_iterations = 0;
    kill_flag = false;
    start_time = std::chrono::steady_clock::now();
    search(0);
}

void dlx_solver::search(int depth) {
    if (kill_flag) {
        return;
    }

    debug_iterations++;

    if (debug_iterations % 10000 == 0) {
        auto current_time = std::chrono::steady_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(current_time - start_time).count();
        if (elapsed > 2500) {
            kill_flag = true;
            fprintf(stderr, "[WARN] DLX Search: Time limit of 2500ms exceeded. Aborting search.\n");
            fflush(stderr);
            return;
        }
    }

    if (root->right == root) {
        final_solution = current_solution;
        return;
    }

    Node* min_column = root->right;
    Node* current_column = min_column->right;

    while (current_column != root) {
        if (current_column->size < min_column->size) {
            min_column = current_column;
        }
        current_column = current_column->right;
    }

    if (min_column->size == 0) {
        return;
    }

    cover(min_column);
    Node* current_row = min_column->down;

    while (current_row != min_column) {
        current_solution.push_back(current_row->row_id);

        for (Node* node = current_row->right; node != current_row; node = node->right) {
            cover(node->column_header);
        }

        search(depth + 1);

        if (kill_flag || !final_solution.empty()) {
            return;
        }

        current_solution.pop_back();

        for (Node* node = current_row->left; node != current_row; node = node->left) {
            uncover(node->column_header);
        }

        current_row = current_row->down;
    }

    uncover(min_column);
}

void dlx_solver::buildGraph(const int *flat_matrix) {
    int col_num = flat_matrix[0];
    int row_num = flat_matrix[1];

    std::vector<Node*> column_headers(col_num);
    Node* prev_col = root;

    for (int i = 0; i < col_num; ++i) {
        Node* col = new Node();
        col->row_id = -1;
        col->size = 0;
        col->up = col;
        col->down = col;
        col->left = prev_col;
        col->right = root;
        col->column_header = col;

        prev_col->right = col;
        root->left = col;
        column_headers[i] = col;
        prev_col = col;
    }

    int idx = 2;
    for (int i = 0; i < row_num; ++i) {
        int current_row_id = i;
        Node* first_in_row = nullptr;
        Node* last_in_row = nullptr;

        for (int j = 0; j < col_num; ++j) {
            int val = flat_matrix[idx++];

            if (val != -1 && (val < 0 || val >= col_num)) {
                fprintf(stderr, "[ERROR] DLX BuildGraph: Index out of bounds (val=%d, max_col=%d).\n", val, col_num);
                fflush(stderr);
                return;
            }

            if (val != -1) {
                Node* column_header = column_headers[val];
                column_header->size++;
                Node* node = new Node();
                node->row_id = current_row_id;
                node->column_header = column_header;

                if (first_in_row == nullptr) {
                    first_in_row = node;
                    last_in_row = node;
                    node->right = node;
                    node->left = node;
                } else {
                    node->left = last_in_row;
                    last_in_row->right = node;
                    last_in_row = node;
                    last_in_row->right = first_in_row;
                    first_in_row->left = last_in_row;
                }

                node->down = column_header;
                node->up = column_header->up;
                column_header->up->down = node;
                column_header->up = node;
            }
        }
    }
}

std::vector<int> dlx_solver::getSolution() const {
    return final_solution;
}

#ifdef _WIN32
#define DLX_EXPORT __declspec(dllexport)
#else
#define DLX_EXPORT
#endif

extern "C" {
    DLX_EXPORT void solveExactCover(const int* flat_matrix, int* solution_buffer, int* solution_size) {
        fprintf(stderr, "[INFO] Native Execution Started. Matrix dimensions: %d columns, %d rows.\n", flat_matrix[0], flat_matrix[1]);
        fflush(stderr);

        dlx_solver solver(flat_matrix);
        fprintf(stderr, "[INFO] Exact Cover graph built successfully. Starting DLX search...\n");
        fflush(stderr);

        solver.solve();

        if (kill_flag) {
            *solution_size = -1;
            fprintf(stderr, "[WARN] Native Execution Aborted due to timeout limit.\n");
            fflush(stderr);
            return;
        }

        fprintf(stderr, "[INFO] DLX search finished.\n");
        fflush(stderr);

        std::vector<int> solution = solver.getSolution();
        *solution_size = solution.size();

        for (size_t i = 0; i < solution.size(); ++i) {
            solution_buffer[i] = solution[i];
        }

        fprintf(stderr, "[INFO] Native Execution Complete. Solution size: %d.\n", *solution_size);
        fflush(stderr);
    }
}