import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { TopCandidatesComponent } from './top-candidates.component';
import { StockValuationService, TopCandidateResponse } from '../../services/stock-valuation.service';

const mockCandidates: TopCandidateResponse[] = [
  {
    cik: 320193,
    name: 'Apple Inc.',
    ticker: 'AAPL',
    periodEnd: '2024-09-28',
    price: 220.5,
    qualityScore: 92.4,
    valueScore: {
      composite: 75.0,
      peTtmPercentile: 70.0,
      pbRatioPercentile: 65.0,
      psRatioPercentile: 72.0,
      pegRatioPercentile: 80.0,
      discountToFairValuePercentile: 88.0
    },
    finalScore: 83.7,
    metrics: [
      {
        metric: 'PE_TTM',
        value: 28.5,
        assessment: { rating: 'NEUTRAL', label: 'Fair P/E' },
        benchmark: { lowerBound: 15, upperBound: 25 },
        description: 'Price-to-Earnings ratio',
        interpretation: 'Currently trading at a fair P/E multiple relative to historic market averages.'
      },
      {
        metric: 'ROE',
        value: 0.15,
        assessment: { rating: 'FAVORABLE', label: 'High ROE' },
        benchmark: { lowerBound: 0.1, upperBound: null },
        description: 'Return on Equity',
        interpretation: 'Strong profitability and return on equity.'
      }
    ]
  },
  {
    cik: 789019,
    name: 'Microsoft Corporation',
    ticker: 'MSFT',
    periodEnd: '2024-06-30',
    price: 430.0,
    qualityScore: 95.0,
    valueScore: {
      composite: 70.0,
      peTtmPercentile: 60.0,
      pbRatioPercentile: 55.0,
      psRatioPercentile: 65.0,
      pegRatioPercentile: 82.0,
      discountToFairValuePercentile: 88.0
    },
    finalScore: 82.5,
    metrics: [
      {
        metric: 'PE_TTM',
        value: 34.2,
        assessment: { rating: 'UNFAVORABLE', label: 'High P/E' },
        benchmark: { lowerBound: 15, upperBound: 25 },
        description: 'Price-to-Earnings ratio'
      }
    ]
  }
];

describe('TopCandidatesComponent', () => {
  let fixture: ComponentFixture<TopCandidatesComponent>;
  let component: TopCandidatesComponent;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TopCandidatesComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads candidates automatically on ngOnInit', () => {
    component.ngOnInit();

    expect(component.loading()).toBe(true);

    const req = httpMock.expectOne('http://localhost:8080/api/candidates/top?limit=20');
    expect(req.request.method).toBe('GET');

    req.flush(mockCandidates);

    expect(component.loading()).toBe(false);
    expect(component.candidates().length).toBe(2);
    expect(component.filteredCandidates().length).toBe(2);
  });

  it('filters candidates based on search query', () => {
    component.candidates.set(mockCandidates);

    // Search by ticker
    component.searchQuery.set('msft');
    expect(component.filteredCandidates().length).toBe(1);
    expect(component.filteredCandidates()[0].ticker).toBe('MSFT');

    // Search by company name
    component.searchQuery.set('apple');
    expect(component.filteredCandidates().length).toBe(1);
    expect(component.filteredCandidates()[0].ticker).toBe('AAPL');

    // Search by CIK
    component.searchQuery.set('789019');
    expect(component.filteredCandidates().length).toBe(1);
    expect(component.filteredCandidates()[0].ticker).toBe('MSFT');

    // Empty search query restores full list
    component.searchQuery.set('');
    expect(component.filteredCandidates().length).toBe(2);
  });

  it('sorts candidates according to selected field and direction', () => {
    component.candidates.set(mockCandidates);

    // Default sort is finalScore desc (AAPL 83.7 > MSFT 82.5)
    expect(component.filteredCandidates()[0].ticker).toBe('AAPL');

    // Sort by qualityScore desc (MSFT 95.0 > AAPL 92.4)
    component.setSort('qualityScore');
    expect(component.sortBy()).toBe('qualityScore');
    expect(component.sortDirection()).toBe('desc');
    expect(component.filteredCandidates()[0].ticker).toBe('MSFT');

    // Toggle same sort field switches to asc
    component.setSort('qualityScore');
    expect(component.sortDirection()).toBe('asc');
    expect(component.filteredCandidates()[0].ticker).toBe('AAPL');

    // Sort by ticker asc
    component.setSort('ticker');
    expect(component.sortBy()).toBe('ticker');
    expect(component.sortDirection()).toBe('asc');
    expect(component.filteredCandidates()[0].ticker).toBe('AAPL');
    expect(component.filteredCandidates()[1].ticker).toBe('MSFT');
  });

  it('selects and deselects a candidate', () => {
    component.candidates.set(mockCandidates);

    expect(component.selectedCandidate()).toBeNull();

    component.selectCandidate(mockCandidates[0]);
    expect(component.selectedCandidate()).toEqual(mockCandidates[0]);

    component.clearSelection();
    expect(component.selectedCandidate()).toBeNull();
  });

  it('retrieves metric response for a given metric id on a candidate', () => {
    const candidate = mockCandidates[0];

    const pe = component.getMetricResult(candidate, 'PE_TTM');
    expect(pe).not.toBeNull();
    expect(pe?.value).toBe(28.5);

    const missingMetric = component.getMetricResult(candidate, 'GRAHAM_NUMBER');
    expect(missingMetric).toBeNull();
  });

  it('handles 503 service unavailable error with informative upstream message', () => {
    component.loadCandidates();

    const req = httpMock.expectOne('http://localhost:8080/api/candidates/top?limit=20');
    req.flush(
      { error: 'A required upstream service is currently unavailable.' },
      { status: 503, statusText: 'Service Unavailable' }
    );

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toContain('Upstream screener service is unavailable');
    expect(component.candidates()).toEqual([]);
  });

  it('emits openInWorkspace output when requested', () => {
    let emittedCandidate: TopCandidateResponse | undefined;
    component.openInWorkspace.subscribe((c) => (emittedCandidate = c));

    component.onSendToWorkspace(mockCandidates[0]);
    expect(emittedCandidate).toEqual(mockCandidates[0]);
  });
});
