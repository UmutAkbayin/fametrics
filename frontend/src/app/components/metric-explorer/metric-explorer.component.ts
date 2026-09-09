import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MetricValuePath, StockValuationService, SummaryRequest } from '../../services/stock-valuation.service';

interface DetailMetricDef {
  id: string;
  name: string;
  abbreviation: string;
  formula: string;
  mathDisplay: string;
  description: string;
  inputs: { name: string; label: string; placeholder: string; validator: any }[];
  interpretationGuide: string;
  benchmark: string;
}

@Component({
  selector: 'app-metric-explorer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
  ],
  templateUrl: './metric-explorer.component.html',
  styleUrls: ['./metric-explorer.component.scss'],
})
export class MetricExplorerComponent implements OnInit {
  selectedMetricId = signal('peTtm');
  playgroundForm!: FormGroup;
  result = signal<number | null>(null);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  // Rich educational documentation & focused calculator inputs for each metric
  metricDetails: Record<string, DetailMetricDef> = {
    peTtm: {
      id: 'peTtm',
      name: 'Price-to-Earnings Ratio',
      abbreviation: 'P/E (TTM)',
      formula: 'Share Price / Earnings Per Share (EPS)',
      mathDisplay: 'P/E = \\frac{\\text{Share Price}}{\\text{EPS}}',
      description:
        "The Price-to-Earnings ratio measures a company's current share price relative to its per-share earnings. It shows how much the market is willing to pay today for a dollar of past earnings. P/E helps investors determine if a stock is overvalued, fairly valued, or undervalued compared to its historical averages, its peers, and the wider market.",
      inputs: [
        {
          name: 'sharePrice',
          label: 'Share Price ($)',
          placeholder: 'e.g. 150',
          validator: [Validators.required, Validators.min(0.01)],
        },
        {
          name: 'eps',
          label: 'Earnings Per Share (EPS, $)',
          placeholder: 'e.g. 10.00',
          validator: [Validators.required, Validators.min(0.01)],
        },
      ],
      interpretationGuide:
        'Historically, a typical market average P/E ranges between 15 and 25. High P/E ratios are usually found in high-growth companies (like technology) because investors expect higher future earnings growth. Low P/E ratios can signal that a stock is a bargain ("value investing") or that the company has structural troubles.',
      benchmark: 'Favorable: < 15.00 | Average: 15.00 - 25.00 | Premium: > 25.00',
    },
    pbRatio: {
      id: 'pb',
      name: 'Price-to-Book Ratio',
      abbreviation: 'P/B',
      formula: 'Share Price / Book Value Per Share (BVPS)',
      mathDisplay: 'P/B = \\frac{\\text{Share Price}}{\\text{BVPS}}',
      description:
        "The Price-to-Book ratio compares a company's market valuation to its book value (historical balance sheet assets minus liabilities). It helps value investors identify situations where a company is priced close to, or even below, the value of its tangible assets.",
      inputs: [
        {
          name: 'sharePrice',
          label: 'Share Price ($)',
          placeholder: 'e.g. 45',
          validator: [Validators.required, Validators.min(0.01)],
        },
        {
          name: 'bvps',
          label: 'Book Value Per Share (BVPS, $)',
          placeholder: 'e.g. 30',
          validator: [Validators.required, Validators.min(0.01)],
        },
      ],
      interpretationGuide:
        "Traditionally, a P/B value below 1.5 is considered solid for value stocks. It means you aren't paying a massive premium for the net assets of the firm. A P/B ratio below 1.0 indicates that a stock is trading for less than its net assets, which occurs during market distress or poor capital returns.",
      benchmark: 'Favorable: < 1.50 | Average: 1.50 - 3.00 | Premium: > 3.00',
    },
    psRatio: {
      id: 'ps',
      name: 'Price-to-Sales Ratio',
      abbreviation: 'P/S',
      formula: 'Market Cap / Total Revenue',
      mathDisplay: 'P/S = \\frac{\\text{Market Capitalization}}{\\text{Total Revenue}}',
      description:
        "The Price-to-Sales ratio compares a company's total market value to its total top-line revenue. This metric is highly valuable for evaluating fast-growing companies or startups that are not yet profitable, where P/E calculations are impossible.",
      inputs: [
        {
          name: 'marketCap',
          label: 'Market Capitalization ($)',
          placeholder: 'e.g. 500000000',
          validator: [Validators.required, Validators.min(0.01)],
        },
        {
          name: 'totalRevenue',
          label: 'Total Revenue ($)',
          placeholder: 'e.g. 100000000',
          validator: [Validators.required, Validators.min(0.01)],
        },
      ],
      interpretationGuide:
        'Lower P/S ratios indicate that the investor pays less for each dollar of revenue generated. A P/S ratio under 1.5 is generally attractive. P/S ratios can vary dramatically by industry: service or software firms have high margins and trade at higher P/S ratios, whereas low-margin supermarkets or retail trade at low P/S ratios.',
      benchmark: 'Favorable: < 1.50 | Average: 1.50 - 4.00 | Premium: > 4.00',
    },
    pegRatio: {
      id: 'peg',
      name: 'Price/Earnings-to-Growth Ratio',
      abbreviation: 'PEG',
      formula: 'P/E Ratio / (Annual EPS Growth Rate × 100)',
      mathDisplay: 'PEG = \\frac{\\text{P/E}}{\\text{EPS Growth Rate} \\times 100}',
      description:
        "The PEG ratio refines the basic P/E ratio by factoring in the company's expected or historical earnings growth rate. P/E alone might look expensive, but if the company is growing at a rapid pace, the stock might actually be cheap relative to its growth.",
      inputs: [
        {
          name: 'sharePrice',
          label: 'Share Price ($)',
          placeholder: 'e.g. 200',
          validator: [Validators.required, Validators.min(0.01)],
        },
        {
          name: 'eps',
          label: 'Earnings Per Share (EPS, $)',
          placeholder: 'e.g. 10.00',
          validator: [Validators.required, Validators.min(0.01)],
        },
        {
          name: 'epsGrowthRate',
          label: 'Annual EPS Growth (%)',
          placeholder: 'e.g. 20 (for 20%)',
          validator: [Validators.required, Validators.min(0.01)],
        },
      ],
      interpretationGuide:
        'A general benchmark is that a PEG ratio of 1.0 represents a fair price. A PEG ratio below 1.0 is considered highly favorable, suggesting the stock is cheap relative to its earnings growth. A PEG above 2.0 suggests growth is already heavily priced in.',
      benchmark: 'Favorable: < 1.00 | Average: 1.00 - 2.00 | Overpriced: > 2.00',
    },
    deRatio: {
      id: 'de',
      name: 'Debt-to-Equity Ratio',
      abbreviation: 'D/E',
      formula: 'Total Liabilities / Total Equity',
      mathDisplay: 'D/E = \\frac{\\text{Total Liabilities}}{\\text{Total Shareholder Equity}}',
      description:
        "The Debt-to-Equity ratio measures a company's financial leverage and capital structuring. It shows the proportion of assets funded by debt versus shareholder equity. High leverage magnifies returns during expansion but creates significant insolvency risk during economic contractions.",
      inputs: [
        {
          name: 'totalLiabilities',
          label: 'Total Liabilities ($)',
          placeholder: 'e.g. 10000000',
          validator: [Validators.required, Validators.min(0)],
        },
        {
          name: 'totalEquity',
          label: 'Total Shareholder Equity ($)',
          placeholder: 'e.g. 20000000',
          validator: [Validators.required, Validators.min(0.01)],
        },
      ],
      interpretationGuide:
        'A lower D/E ratio indicates a financially stable company that relies less on borrowed capital. A ratio below 1.0 is considered conservative and safe. Debt levels vary by sector: capital-heavy sectors like utilities and manufacturing naturally carry higher D/E ratios than tech firms.',
      benchmark: 'Conservative: < 1.00 | Moderate: 1.00 - 2.00 | Elevated Risk: > 2.00',
    },
    roeRatio: {
      id: 'roe',
      name: 'Return on Equity',
      abbreviation: 'ROE',
      formula: 'Net Income / Total Equity',
      mathDisplay: 'ROE = \\frac{\\text{Net Income}}{\\text{Total Shareholder Equity}} \\times 100',
      description:
        "Return on Equity measures corporate profitability by showing how much profit a company generates with the money shareholders have invested. It reflects management's capability to turn capital injections into bottom-line profits.",
      inputs: [
        {
          name: 'netIncome',
          label: 'Net Income ($)',
          placeholder: 'e.g. 3000000',
          validator: [Validators.required],
        },
        {
          name: 'totalEquity',
          label: 'Total Shareholder Equity ($)',
          placeholder: 'e.g. 20000000',
          validator: [Validators.required, Validators.min(0.01)],
        },
      ],
      interpretationGuide:
        'An ROE of 15% or higher is considered excellent. It demonstrates powerful efficiency in utilizing reinvested capital. A dropping ROE can indicate management is wasting shareholder capital on low-return projects.',
      benchmark: 'Excellent: >= 15% | Average: 8% - 15% | Weak: < 8%',
    },
    grahamNumber: {
      id: 'graham',
      name: 'Graham Number',
      abbreviation: 'Graham #',
      formula: '√(22.5 × EPS × BVPS)',
      mathDisplay: 'V_{\\text{Graham}} = \\sqrt{22.5 \\times \\text{EPS} \\times \\text{BVPS}}',
      description:
        'The Graham Number represents the theoretical upper limit of what defensive investors should pay for a stock, formulated by Benjamin Graham (the father of value investing and mentor to Warren Buffett). The constant 22.5 comes from multiplying a maximum P/E of 15 by a maximum P/B of 1.5.',
      inputs: [
        {
          name: 'eps',
          label: 'Earnings Per Share (EPS, $)',
          placeholder: 'e.g. 4.00',
          validator: [Validators.required, Validators.min(0.01)],
        },
        {
          name: 'bvps',
          label: 'Book Value Per Share (BVPS, $)',
          placeholder: 'e.g. 25.00',
          validator: [Validators.required, Validators.min(0.01)],
        },
      ],
      interpretationGuide:
        'If the current market price of the stock is lower than its Graham Number, the stock is considered mathematically undervalued, offering a "margin of safety". If it is higher, the stock is priced at a premium.',
      benchmark: 'Stock Price < Graham Number = Undervalued Discount',
    },
    lynchFairValue: {
      id: 'lynch',
      name: 'Peter Lynch Fair Value',
      abbreviation: 'Lynch FV',
      formula: 'EPS × (Annual Growth Rate × 100)',
      mathDisplay: 'V_{\\text{Lynch}} = \\text{EPS} \\times (\\text{EPS Growth Rate} \\times 100)',
      description:
        "Peter Lynch, the legendary manager of the Fidelity Magellan Fund, argued that a stock's fair valuation (fair P/E) is exactly equal to its long-term annual growth rate. If a company grows at 15% per year, it is fairly priced at a P/E of 15. The fair value price is therefore calculated by multiplying the earnings by the growth percentage.",
      inputs: [
        {
          name: 'eps',
          label: 'Earnings Per Share (EPS, $)',
          placeholder: 'e.g. 5.00',
          validator: [Validators.required, Validators.min(0.01)],
        },
        {
          name: 'epsGrowthRate',
          label: 'Annual Growth Rate (%)',
          placeholder: 'e.g. 15 (for 15%)',
          validator: [Validators.required],
        },
      ],
      interpretationGuide:
        'If the Peter Lynch Fair Value is greater than the current stock price, the stock is a potential buy. If the P/E is half of the growth rate (ratio >= 2.0), Lynch considered it exceptionally cheap. P/E ratios double the growth rate are expensive.',
      benchmark: 'FV > Current Price = Potential Value Buy',
    },
  };

  selectedMetric = computed<DetailMetricDef>(() => this.metricDetails[this.selectedMetricId()]);

  private readonly metricPaths: Record<string, MetricValuePath> = {
    peTtm: 'pe-ttm',
    pbRatio: 'pb',
    psRatio: 'ps',
    pegRatio: 'peg',
    deRatio: 'de',
    roeRatio: 'roe',
    grahamNumber: 'graham',
    lynchFairValue: 'lynch',
  };

  constructor(
    private fb: FormBuilder,
    private valuationService: StockValuationService,
  ) {}

  ngOnInit() {
    this.selectMetric(this.selectedMetricId());
  }

  selectMetric(metricId: string) {
    this.selectedMetricId.set(metricId);
    this.result.set(null);
    this.errorMessage.set(null);

    const group: Record<string, any> = {};
    const details = this.metricDetails[metricId];

    details.inputs.forEach((input) => {
      group[input.name] = [null, input.validator];
    });

    this.playgroundForm = this.fb.group(group);
  }

  private buildSummaryRequest(values: Record<string, any>): SummaryRequest {
    return {
      marketData: {
        sharePrice: values['sharePrice'] ?? null,
        eps: values['eps'] ?? null,
        bvps: values['bvps'] ?? null,
      },
      fundamentalData: {
        marketCap: values['marketCap'] ?? null,
        totalRevenue: values['totalRevenue'] ?? null,
        // Form collects growth as a whole percentage (e.g. 20 for 20%); API wants the fractional rate.
        epsGrowthRate: values['epsGrowthRate'] != null ? values['epsGrowthRate'] / 100 : null,
      },
      capitalStructure: {
        totalLiabilities: values['totalLiabilities'] ?? null,
        totalEquity: values['totalEquity'] ?? null,
        netIncome: values['netIncome'] ?? null,
      },
    };
  }

  calculate() {
    if (this.playgroundForm.invalid) {
      this.errorMessage.set('Please provide valid inputs to calculate.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.result.set(null);

    const path = this.metricPaths[this.selectedMetricId()];
    const request = this.buildSummaryRequest(this.playgroundForm.value);

    this.valuationService.calculateMetricValue(path, request).subscribe({
      next: (res) => {
        this.result.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(
          `Failed to calculate ${this.selectedMetric().abbreviation}: ` +
            (err.error ?? err.message ?? err.statusText ?? 'Unknown error'),
        );
        this.loading.set(false);
      },
    });
  }
}
