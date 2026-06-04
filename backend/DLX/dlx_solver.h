//
// Created by arman on 5/31/2026.
//

#ifndef DLX_DLX_SOLVER_H
#define DLX_DLX_SOLVER_H

#include <vector>

struct Node {
    Node* left;
    Node* right;
    Node* up;
    Node* down;
    Node* column_header;
    int row_id;
    int size;
};

class dlx_solver {
public:
    dlx_solver(const int* flat_matrix);
    ~dlx_solver();
    void solve();
    std::vector<int> getSolution() const;

private:
    Node* root;
    std::vector<int> current_solution;
    std::vector<int> final_solution;
    void buildGraph(const int* flat_matrix);
    void search(int depth);

    void cover(Node* column);
    void uncover(Node* column);
    void cleanup();
};

extern "C" {
    void solveExactCover(const int* flat_matrix, int* solution_buffer, int* solution_size);
}

#endif //DLX_DLX_SOLVER_H