import { Component, OnInit, computed, output, signal } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MetricCardComponent } from '../metric-card/metric-card.component';
import {
  StockValuationService,
  TopCandidateResponse,
  MetricDef,
  METRIC_DEFINITIONS,
  MetricResponse,
  MetricType
} from '../../services/stock-valuation.service';

export type SortField = 'finalScore' | 'qualityScore' | 'valueScore' | 'ticker' | 'price';

@Component({
  selector: 'app-top-candidates',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DecimalPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MetricCardComponent
  ],
  templateUrl: './top-candidates.component.html',
  styleUrls: ['./top-candidates.component.scss']
})
export class TopCandidatesComponent implements OnInit {
  candidates = signal<TopCandidateResponse[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  searchQuery = signal<string>('');
  selectedCandidate = signal<TopCandidateResponse | null>(null);
  sortBy = signal<SortField>('finalScore');
  sortDirection = signal<'asc' | 'desc'>('desc');

  readonly metrics: MetricDef[] = METRIC_DEFINITIONS;

  // Output event when user wants to transfer a candidate to the interactive workspace
  openInWorkspace = output<TopCandidateResponse>();

  filteredCandidates = computed(() => {
    const list = this.candidates();
    const query = this.searchQuery().trim().toLowerCase();
    const sort = this.sortBy();
    const dir = this.sortDirection();

    let filtered = list;
    if (query) {
      filtered = list.filter(
        (c) =>
          c.ticker.toLowerCase().includes(query) ||
          c.name.toLowerCase().includes(query) ||
          c.cik.toString().includes(query)
      );
    }

    return [...filtered].sort((a, b) => {
      let comparison = 0;
      switch (sort) {
        case 'finalScore':
          comparison = (a.finalScore ?? 0) - (b.finalScore ?? 0);
          break;
        case 'qualityScore':
          comparison = (a.qualityScore ?? 0) - (b.qualityScore ?? 0);
          break;
        case 'valueScore':
          comparison = (a.valueScore?.composite ?? 0) - (b.valueScore?.composite ?? 0);
          break;
        case 'price':
          comparison = (a.price ?? 0) - (b.price ?? 0);
          break;
        case 'ticker':
          comparison = a.ticker.localeCompare(b.ticker);
          break;
      }
      return dir === 'desc' ? -comparison : comparison;
    });
  });

  constructor(private valuationService: StockValuationService) {}

  ngOnInit(): void {
    this.loadCandidates();
  }

  loadCandidates(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.valuationService.getTopCandidates(20).subscribe({
      next: (data) => {
        this.candidates.set(data || []);
        this.loading.set(false);

        // Keep current selection if still present in new list, else keep null or preserve
        const current = this.selectedCandidate();
        if (current) {
          const match = data.find((c) => c.cik === current.cik);
          this.selectedCandidate.set(match ?? null);
        }
      },
      error: (err) => {
        this.loading.set(false);
        const backendMessage = err?.error?.error || err?.message;
        const msg =
          err.status === 503
            ? `Upstream screener service is unavailable: ${backendMessage || 'Please verify the screener service is running on port 8081.'}`
            : (backendMessage || 'Failed to load top candidates.');
        this.errorMessage.set(msg);
      }
    });
  }

  selectCandidate(candidate: TopCandidateResponse): void {
    this.selectedCandidate.set(candidate);
  }

  clearSelection(): void {
    this.selectedCandidate.set(null);
  }

  setSort(field: SortField): void {
    if (this.sortBy() === field) {
      this.sortDirection.update((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortBy.set(field);
      this.sortDirection.set(field === 'ticker' ? 'asc' : 'desc');
    }
  }

  getMetricResult(candidate: TopCandidateResponse, metricId: MetricType): MetricResponse | null {
    if (!candidate?.metrics) return null;
    return candidate.metrics.find((m) => m.metric === metricId) ?? null;
  }

  onSendToWorkspace(candidate: TopCandidateResponse): void {
    this.openInWorkspace.emit(candidate);
  }
}
