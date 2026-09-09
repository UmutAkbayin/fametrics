import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { StockValuationService, SummaryRequest, SummaryResponse } from './stock-valuation.service';

describe('StockValuationService', () => {
  let service: StockValuationService;
  let httpMock: HttpTestingController;

  const apiUrl = 'http://localhost:8080/api/metrics';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(StockValuationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('posts to the metric-specific endpoint and returns the decimal value', () => {
    const request: SummaryRequest = {
      marketData: { sharePrice: 150, eps: 10, bvps: null },
    };

    let result: number | undefined;
    service.calculateMetricValue('pe-ttm', request).subscribe((res) => (result = res));

    const req = httpMock.expectOne(`${apiUrl}/pe-ttm`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);

    req.flush(15);

    expect(result).toBe(15);
  });

  it('builds the URL from whichever metric path is passed in', () => {
    service.calculateMetricValue('graham', {}).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/graham`);
    req.flush(42);
  });

  it('posts to the summary/assessment endpoint and returns the full response', () => {
    const request: SummaryRequest = {
      marketData: { sharePrice: 150, eps: 10, bvps: 45 },
    };
    const mockResponse: SummaryResponse = {
      metrics: [
        {
          metric: 'PE_TTM',
          value: 15,
          assessment: { rating: 'NEUTRAL', label: 'Fairly Valued' },
        },
      ],
    };

    let result: SummaryResponse | undefined;
    service.calculateSummary(request).subscribe((res) => (result = res));

    const req = httpMock.expectOne(`${apiUrl}/summary/assessment`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);

    req.flush(mockResponse);

    expect(result).toEqual(mockResponse);
  });
});
