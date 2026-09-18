import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

// ==========================================
// OpenAPI 3.0.3 Schema Request & Response Types
// ==========================================

export type MetricType =
  | 'PE_TTM'
  | 'PB_RATIO'
  | 'PS_RATIO'
  | 'PEG_RATIO'
  | 'DE_RATIO'
  | 'ROE'
  | 'GRAHAM_NUMBER'
  | 'LYNCH_FAIR_VALUE';

export type Rating = 'FAVORABLE' | 'NEUTRAL' | 'UNFAVORABLE' | 'NOT_MEANINGFUL';

// URL path segments for the single-metric "value only" endpoints, e.g. POST /api/metrics/pe-ttm.
export type MetricValuePath = 'pe-ttm' | 'pb' | 'ps' | 'peg' | 'de' | 'roe' | 'graham' | 'lynch';

export interface MarketData {
  sharePrice?: number | null;
  eps?: number | null;
  bvps?: number | null;
}

export interface FundamentalData {
  marketCap?: number | null;
  totalRevenue?: number | null;
  epsGrowthRate?: number | null; // Fractional growth rate (e.g. 0.15 for 15%). May be negative.
}

export interface CapitalStructure {
  totalLiabilities?: number | null;
  totalEquity?: number | null;
  netIncome?: number | null; // May be negative (a loss).
}

export interface SummaryRequest {
  marketData?: Partial<MarketData>;
  fundamentalData?: Partial<FundamentalData>;
  capitalStructure?: Partial<CapitalStructure>;
}

export interface Assessment {
  rating?: Rating;
  label?: string;
}

export interface Benchmark {
  lowerBound?: number | null;
  upperBound?: number | null;
  explanation?: string;
}

export interface MetricResponse {
  metric: MetricType;
  value: number;
  assessment?: Assessment;
  benchmark?: Benchmark;
  description?: string;
  interpretation?: string;
}

export interface SummaryResponse {
  metrics: MetricResponse[];
}

export interface ValueScore {
  peTtmPercentile?: number | null;
  pbRatioPercentile?: number | null;
  psRatioPercentile?: number | null;
  pegRatioPercentile?: number | null;
  discountToFairValuePercentile?: number | null;
  composite?: number | null;
}

export interface TopCandidateResponse {
  cik: number;
  name: string;
  ticker: string;
  periodEnd: string;
  price: number;
  qualityScore: number;
  valueScore: ValueScore;
  finalScore: number;
  metrics: MetricResponse[];
}

export interface ErrorResponse {
  error: string;
}

export interface MetricDef {
  id: MetricType;
  title: string;
  abbreviation: string;
  formula: string;
  required: string[];
}

export const METRIC_DEFINITIONS: MetricDef[] = [
  { id: 'PE_TTM', title: 'Price-to-Earnings Ratio', abbreviation: 'P/E (TTM)', formula: 'Price / EPS', required: ['sharePrice', 'eps'] },
  { id: 'PB_RATIO', title: 'Price-to-Book Ratio', abbreviation: 'P/B', formula: 'Price / BVPS', required: ['sharePrice', 'bvps'] },
  { id: 'PS_RATIO', title: 'Price-to-Sales Ratio', abbreviation: 'P/S', formula: 'Market Cap / Revenue', required: ['marketCap', 'totalRevenue'] },
  { id: 'PEG_RATIO', title: 'Price/Earnings-to-Growth', abbreviation: 'PEG', formula: 'P/E / (Growth × 100)', required: ['sharePrice', 'eps', 'epsGrowthRate'] },
  { id: 'DE_RATIO', title: 'Debt-to-Equity Ratio', abbreviation: 'D/E', formula: 'Liabilities / Equity', required: ['totalLiabilities', 'totalEquity'] },
  { id: 'ROE', title: 'Return on Equity', abbreviation: 'ROE', formula: 'Net Income / Equity', required: ['netIncome', 'totalEquity'] },
  { id: 'GRAHAM_NUMBER', title: 'Graham Number', abbreviation: 'Graham #', formula: '√(22.5 × EPS × BVPS)', required: ['eps', 'bvps'] },
  { id: 'LYNCH_FAIR_VALUE', title: 'Peter Lynch Fair Value', abbreviation: 'Lynch FV', formula: 'EPS × (Growth × 100)', required: ['eps', 'epsGrowthRate'] }
];

@Injectable({
  providedIn: 'root'
})
export class StockValuationService {
  private apiUrl = 'http://localhost:8080/api/metrics';
  private candidatesApiUrl = 'http://localhost:8080/api/candidates';

  constructor(private http: HttpClient) { }
  // ==========================================
  // Summary Assessment API
  // ==========================================

  calculateSummary(req: SummaryRequest): Observable<SummaryResponse> {
    return this.http.post<SummaryResponse>(`${this.apiUrl}/summary/assessment`, req);
  }

  // ==========================================
  // Single-Metric "Value Only" Endpoints
  // ==========================================
  // Every /api/metrics/{path} endpoint accepts the same SummaryRequest shape
  // and returns just the calculated decimal value (see openapi spec).

  calculateMetricValue(path: MetricValuePath, req: SummaryRequest): Observable<number> {
    return this.http.post<number>(`${this.apiUrl}/${path}`, req);
  }

  // ==========================================
  // Candidates Orchestration API
  // ==========================================

  getTopCandidates(limit: number = 20): Observable<TopCandidateResponse[]> {
    const params = limit ? { limit: limit.toString() } : undefined;
    return this.http.get<TopCandidateResponse[]>(`${this.candidatesApiUrl}/top`, { params });
  }
}

