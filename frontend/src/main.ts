import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { ProverComponent } from './prover.component';

bootstrapApplication(ProverComponent, {
    providers: [
        provideHttpClient()
    ]
}).catch(err => console.error(err));