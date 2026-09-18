import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MetricCardComponent } from '../metric-card/metric-card.component';
import {
  StockValuationService,
  SummaryResponse,
  SummaryRequest,
  MetricType,
  MetricResponse,
  MetricDef,
  METRIC_DEFINITIONS,
  TopCandidateResponse
} from '../../services/stock-valuation.service';

@Component({
  selector: 'app-metric-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MetricCardComponent
  ],
  templateUrl: './metric-dashboard.component.html',
  styleUrls: ['./metric-dashboard.component.scss']
})
export class MetricDashboardComponent implements OnInit {
  inputForm!: FormGroup;
  loading = signal(false);
  results = signal<SummaryResponse | null>(null);
  errorMessage = signal<string | null>(null);

  metrics: MetricDef[] = METRIC_DEFINITIONS;

  constructor(
    private fb: FormBuilder,
    private valuationService: StockValuationService,
  ) {}

  ngOnInit() {
    this.initForm();
  }

  private initForm() {
    this.inputForm = this.fb.group({
      sharePrice: [null, [Validators.min(0.01)]],
      eps: [null, [Validators.min(0.01)]],
      bvps: [null, [Validators.min(0.01)]],
      marketCap: [null, [Validators.min(0.01)]],
      totalRevenue: [null, [Validators.min(0.01)]],
      epsGrowthRate: [null, [Validators.min(0.01), Validators.max(100)]],
      totalLiabilities: [null, [Validators.min(0)]],
      totalEquity: [null, [Validators.min(0.01)]],
      netIncome: [null] // netIncome can be negative (representing a loss)
    });
  }

  getFieldDisplayName(fieldName: string): string {
    const names: Record<string, string> = {
      sharePrice: 'Price',
      eps: 'EPS',
      bvps: 'BVPS',
      marketCap: 'Cap',
      totalRevenue: 'Revenue',
      epsGrowthRate: 'Growth',
      totalLiabilities: 'Liab.',
      totalEquity: 'Equity',
      netIncome: 'Income'
    };
    return names[fieldName] || fieldName;
  }

  getMissingInputs(metricId: MetricType): string[] {
    const metric = this.metrics.find(m => m.id === metricId);
    if (!metric) return [];

    return metric.required.filter(field => {
      const control = this.inputForm.get(field);
      return control === null || control.value === null || control.value === '' || control.invalid;
    }).map(field => this.getFieldDisplayName(field));
  }

  getMetricResult(metricId: MetricType): MetricResponse | null {
    const results = this.results();
    if (!results) return null;
    const metricsList: MetricResponse[] = Array.isArray(results)
      ? results
      : (results as any).metrics;

    if (!Array.isArray(metricsList)) return null;
    return metricsList.find(m => m.metric === metricId) ?? null;
  }

  get currentSharePrice(): number | null {
    return this.inputForm.get('sharePrice')?.value;
  }

  calculate() {
    if (this.inputForm.invalid) {
      this.errorMessage.set('Please fix the validation errors in the inputs panel first.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const formVals = this.inputForm.value;

    const request: SummaryRequest = {
      marketData: {
        sharePrice: formVals.sharePrice || null,
        eps: formVals.eps || null,
        bvps: formVals.bvps || null
      },
      fundamentalData: {
        marketCap: formVals.marketCap || null,
        totalRevenue: formVals.totalRevenue || null,
        epsGrowthRate: formVals.epsGrowthRate != null && formVals.epsGrowthRate !== '' ? formVals.epsGrowthRate / 100 : null
      },
      capitalStructure: {
        totalLiabilities: formVals.totalLiabilities != null && formVals.totalLiabilities !== '' ? formVals.totalLiabilities : null,
        totalEquity: formVals.totalEquity || null,
        netIncome: formVals.netIncome != null && formVals.netIncome !== '' ? formVals.netIncome : null
      }
    };

    this.valuationService.calculateSummary(request).subscribe({
      next: (res) => {
        this.results.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'An error occurred during calculations.');
        this.loading.set(false);
      }
    });
  }

  loadSampleData() {
    this.inputForm.patchValue({
      sharePrice: 150.00,
      eps: 7.50,
      bvps: 45.00,
      marketCap: 750000000,
      totalRevenue: 50000000,
      epsGrowthRate: 12, // 12%
      totalLiabilities: 20000000,
      totalEquity: 80000000,
      netIncome: 12000000
    });
    this.calculate();
  }

  prefillFromCandidate(candidate: TopCandidateResponse) {
    const pe = candidate.metrics?.find((m) => m.metric === 'PE_TTM')?.value;
    const pb = candidate.metrics?.find((m) => m.metric === 'PB_RATIO')?.value;
    const eps = pe && candidate.price ? +(candidate.price / pe).toFixed(2) : null;
    const bvps = pb && candidate.price ? +(candidate.price / pb).toFixed(2) : null;

    this.inputForm.patchValue({
      sharePrice: candidate.price,
      eps,
      bvps
    });
  }

  reset() {
    this.inputForm.reset();
    this.results.set(null);
    this.errorMessage.set(null);
  }
}

