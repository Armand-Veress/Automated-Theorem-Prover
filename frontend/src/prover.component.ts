import { Component, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { environment } from './environments/environment';

interface ProverExample {
    name: string;
    axioms: string;
    theorem: string;
}

interface ProverResponse {
    valid: boolean;
    counterexample: Record<string, boolean> | null;
    solvedBy: string;
    matrixRows: number;
    matrixCols: number;
    executionTimeMs: number;
}

interface CounterexampleEntry {
    key: string;
    value: boolean;
}

@Component({
    selector: 'app-prover',
    templateUrl: './prover.component.html',
    styleUrls: ['./prover.component.css'],
    standalone: true,
    imports: [FormsModule, CommonModule]
})
export class ProverComponent {
    lineNumbersArray = computed(() => {
        const text = this.axiomsText() || '';
        const linesCount = text.split('\n').length;
        return new Array(linesCount);
    });

    private http = inject(HttpClient);

    axiomsText = signal<string>('');
    theoremText = signal<string>('');
    loading = signal<boolean>(false);
    result = signal<ProverResponse | null>(null);

    isOutputValid = computed(() => this.result()?.valid ?? false);

    isTimeout = computed(() => {
        const res = this.result();
        return !!res && !!res.counterexample && res.counterexample['TIMEOUT_EXCEEDED'] === true;
    });

    isCrash = computed(() => {
        const res = this.result();
        return !!res && !!res.counterexample && res.counterexample['BACKEND_CRASH'] === true;
    });

    counterexampleArray = computed<CounterexampleEntry[]>(() => {
        const res = this.result();
        if (!res || !res.counterexample) return [];
        return Object.entries(res.counterexample).map(([key, value]: [string, boolean]) => ({ key, value }));
    });

    private apiUrl = `${environment.apiUrl}/prover/verify`;

    onProve(): void {
        const currentTheorem = this.theoremText().trim();
        if (!currentTheorem) {
            return;
        }

        this.loading.set(true);
        this.result.set(null);

        const axiomsList = this.axiomsText()
            .split('\n')
            .map((line: string) => line.trim())
            .filter((line: string) => line.length > 0);

        const payload = {
            axioms: axiomsList,
            theorem: currentTheorem
        };

        this.http.post<ProverResponse>(this.apiUrl, payload).subscribe({
            next: (response: ProverResponse) => {
                this.result.set(response);
                this.loading.set(false);
            },
            error: (err: any) => {
                console.error(err);
                this.loading.set(false);
            }
        });
    }

    syncScroll(event: Event, lineNumbers: HTMLElement): void {
        const textarea = event.target as HTMLTextAreaElement;
        lineNumbers.scrollTop = textarea.scrollTop;
    }

    examples: ProverExample[] = [
        {
            name: 'Custom Input...',
            axioms: '',
            theorem: ''
        },
        {
            name: 'Math 1: Modus Tollens (Valid)',
            axioms: 'A IMPLIES B\nNOT B',
            theorem: 'NOT A'
        },
        {
            name: 'Math 2: De Morgan\'s Law (Valid)',
            axioms: 'P IFF ( A AND B )',
            theorem: 'NOT P IFF ( NOT A OR NOT B )'
        },
        {
            name: 'Math 3: Exclusive Disjunction (Invalid)',
            axioms: 'A XOR B\nA',
            theorem: 'B'
        },
        {
            name: 'Math 4: Graph Transitivity (Valid)',
            axioms: 'FORALL x FORALL y FORALL z ( Edge(x, y) AND Edge(y, z) IMPLIES Path(x, z) )\nEdge(nodea, nodeb)\nEdge(nodeb, nodec)',
            theorem: 'Path(nodea, nodec)'
        },
        {
            name: 'Math 5: Skolemization & Mixed Quantifiers (Invalid)',
            axioms: 'FORALL x ( Number(x) IMPLIES EXISTS y Greater(y, x) )\nNumber(zero)',
            theorem: 'EXISTS z Greater(zero, z)'
        },
        {
            name: 'Applied 1: System Hardening (Valid)',
            axioms: 'FirewallUp AND PortClosed IMPLIES Secure\nAttackDetected IMPLIES PortClosed\nFirewallUp AND AttackDetected',
            theorem: 'Secure'
        },
        {
            name: 'Applied 2: Access Control Protocol (Invalid)',
            axioms: 'Admin(user) OR Guest(user)\nAdmin(user) IMPLIES CanRead(user) AND CanWrite(user)\nGuest(user) IMPLIES CanRead(user)',
            theorem: 'CanWrite(user)'
        },
        {
            name: 'Applied 3: Cloud Node Failover (Valid)',
            axioms: 'FORALL x ( Server(x) AND Overloaded(x) IMPLIES Migrates(x) )\nServer(serveralpha)\nOverloaded(serveralpha)',
            theorem: 'Migrates(serveralpha)'
        },
        {
            name: 'Applied 4: Network Routing State (Invalid)',
            axioms: 'FORALL x ( Reachable(x) IFF EXISTS y ( Connected(x, y) AND Active(y) ) )\nConnected(routera, switchb)',
            theorem: 'Reachable(routera)'
        },
        {
            name: 'Benchmark 1: 12-Link Heavy Cyclic Chain (~146 Rows)',
            axioms: '( A OR B ) AND ( NOT A OR NOT B )\n( B OR C ) AND ( NOT B OR NOT C )\n( C OR D ) AND ( NOT C OR NOT D )\n( D OR E ) AND ( NOT D OR NOT E )\n( E OR F ) AND ( NOT E OR NOT F )\n( F OR G ) AND ( NOT F OR NOT G )\n( G OR H ) AND ( NOT G OR NOT H )\n( H OR I ) AND ( NOT H OR NOT I )\n( I OR J ) AND ( NOT I OR NOT J )\n( J OR K ) AND ( NOT J OR NOT K )\n( K OR L ) AND ( NOT K OR NOT L )',
            theorem: 'A IFF L'
        },
        {
            name: 'Benchmark 2: Pigeonhole Principle 3x2 (48 Dense Clauses)',
            axioms: 'P11 OR P12\nP21 OR P22\nP31 OR P32\nNOT P11 OR NOT P21\nNOT P11 OR NOT P31\nNOT P21 OR NOT P31\nNOT P12 OR NOT P22\nNOT P12 OR NOT P32\nNOT P22 OR NOT P32',
            theorem: 'P11 AND NOT P11'
        },
        {
            name: 'Benchmark 3: Pigeonhole Principle 4x3 ',
            axioms: 'P11 OR P12 OR P13\nP21 OR P22 OR P23\nP31 OR P32 OR P33\nP41 OR P42 OR P43\nNOT P11 OR NOT P21\nNOT P11 OR NOT P31\nNOT P11 OR NOT P41\nNOT P21 OR NOT P31\nNOT P21 OR NOT P41\nNOT P31 OR NOT P41\nNOT P12 OR NOT P22\nNOT P12 OR NOT P32\nNOT P12 OR NOT P42\nNOT P22 OR NOT P32\nNOT P22 OR NOT P42\nNOT P32 OR NOT P42\nNOT P13 OR NOT P23\nNOT P13 OR NOT P33\nNOT P13 OR NOT P43\nNOT P23 OR NOT P33\nNOT P23 OR NOT P43\nNOT P33 OR NOT P43',
            theorem: 'P11 AND NOT P11'
        },
        {
            name: 'Benchmark 4: Pigeonhole 5x4 ',
            axioms: 'P11 OR P12 OR P13 OR P14\nP21 OR P22 OR P23 OR P24\nP31 OR P32 OR P33 OR P34\nP41 OR P42 OR P43 OR P44\nP51 OR P52 OR P53 OR P54\nNOT P11 OR NOT P21\nNOT P11 OR NOT P31\nNOT P11 OR NOT P41\nNOT P11 OR NOT P51\nNOT P21 OR NOT P31\nNOT P21 OR NOT P41\nNOT P21 OR NOT P51\nNOT P31 OR NOT P41\nNOT P31 OR NOT P51\nNOT P41 OR NOT P51\nNOT P12 OR NOT P22\nNOT P12 OR NOT P32\nNOT P12 OR NOT P42\nNOT P12 OR NOT P52\nNOT P22 OR NOT P32\nNOT P22 OR NOT P42\nNOT P22 OR NOT P52\nNOT P32 OR NOT P42\nNOT P32 OR NOT P52\nNOT P42 OR NOT P52\nNOT P13 OR NOT P23\nNOT P13 OR NOT P33\nNOT P13 OR NOT P43\nNOT P13 OR NOT P53\nNOT P23 OR NOT P33\nNOT P23 OR NOT P43\nNOT P23 OR NOT P53\nNOT P33 OR NOT P43\nNOT P33 OR NOT P53\nNOT P43 OR NOT P53\nNOT P14 OR NOT P24\nNOT P14 OR NOT P34\nNOT P14 OR NOT P44\nNOT P14 OR NOT P54\nNOT P24 OR NOT P34\nNOT P24 OR NOT P44\nNOT P24 OR NOT P54\nNOT P34 OR NOT P44\nNOT P34 OR NOT P54\nNOT P44 OR NOT P54',
            theorem: 'P11 AND NOT P11'
        },
        {
            name: 'Benchmark 5: Pigeonhole 6x5 ',
            axioms: 'P11 OR P12 OR P13 OR P14 OR P15\nP21 OR P22 OR P23 OR P24 OR P25\nP31 OR P32 OR P33 OR P34 OR P35\nP41 OR P42 OR P43 OR P44 OR P45\nP51 OR P52 OR P53 OR P54 OR P55\nP61 OR P62 OR P63 OR P64 OR P65\nNOT P11 OR NOT P21\nNOT P11 OR NOT P31\nNOT P11 OR NOT P41\nNOT P11 OR NOT P51\nNOT P11 OR NOT P61\nNOT P21 OR NOT P31\nNOT P21 OR NOT P41\nNOT P21 OR NOT P51\nNOT P21 OR NOT P61\nNOT P31 OR NOT P41\nNOT P31 OR NOT P51\nNOT P31 OR NOT P61\nNOT P41 OR NOT P51\nNOT P41 OR NOT P61\nNOT P51 OR NOT P61\nNOT P12 OR NOT P22\nNOT P12 OR NOT P32\nNOT P12 OR NOT P42\nNOT P12 OR NOT P52\nNOT P12 OR NOT P62\nNOT P22 OR NOT P32\nNOT P22 OR NOT P42\nNOT P22 OR NOT P52\nNOT P22 OR NOT P62\nNOT P32 OR NOT P42\nNOT P32 OR NOT P52\nNOT P32 OR NOT P62\nNOT P42 OR NOT P52\nNOT P42 OR NOT P62\nNOT P52 OR NOT P62\nNOT P13 OR NOT P23\nNOT P13 OR NOT P33\nNOT P13 OR NOT P43\nNOT P13 OR NOT P53\nNOT P13 OR NOT P63\nNOT P23 OR NOT P33\nNOT P23 OR NOT P43\nNOT P23 OR NOT P53\nNOT P23 OR NOT P63\nNOT P33 OR NOT P43\nNOT P33 OR NOT P53\nNOT P33 OR NOT P63\nNOT P43 OR NOT P53\nNOT P43 OR NOT P63\nNOT P53 OR NOT P63\nNOT P14 OR NOT P24\nNOT P14 OR NOT P34\nNOT P14 OR NOT P44\nNOT P14 OR NOT P54\nNOT P14 OR NOT P64\nNOT P24 OR NOT P34\nNOT P24 OR NOT P44\nNOT P24 OR NOT P54\nNOT P24 OR NOT P64\nNOT P34 OR NOT P44\nNOT P34 OR NOT P54\nNOT P34 OR NOT P64\nNOT P44 OR NOT P54\nNOT P44 OR NOT P64\nNOT P54 OR NOT P64\nNOT P15 OR NOT P25\nNOT P15 OR NOT P35\nNOT P15 OR NOT P45\nNOT P15 OR NOT P55\nNOT P15 OR NOT P65\nNOT P25 OR NOT P35\nNOT P25 OR NOT P45\nNOT P25 OR NOT P55\nNOT P25 OR NOT P65\nNOT P35 OR NOT P45\nNOT P35 OR NOT P55\nNOT P35 OR NOT P65\nNOT P45 OR NOT P55\nNOT P45 OR NOT P65\nNOT P55 OR NOT P65',
            theorem: 'P11 AND NOT P11'
        }
    ];

    selectedExample: ProverExample = this.examples[0];

    onExampleChange(example: ProverExample): void {
        this.selectedExample = example;
        this.axiomsText.set(example.axioms);
        this.theoremText.set(example.theorem);
        this.result.set(null);
    }
}