import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { MetricExplorerComponent } from './metric-explorer.component';

describe('MetricExplorerComponent', () => {
  let component: MetricExplorerComponent;
  let httpMock: HttpTestingController;

  const apiUrl = 'http://localhost:8080/api/metrics';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(MetricExplorerComponent);
    component = fixture.componentInstance;
    component.selectMetric('peTtm');
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('refuses to calculate when the form is invalid, without firing a request', () => {
    component.calculate();

    expect(component.errorMessage()).toBe('Please provide valid inputs to calculate.');
    expect(component.loading()).toBe(false);
    httpMock.expectNone(`${apiUrl}/pe-ttm`);
  });

  it('posts the mapped endpoint and nested request body, then stores the result', () => {
    component.playgroundForm.patchValue({ sharePrice: 150, eps: 10 });
    component.calculate();

    expect(component.loading()).toBe(true);

    const req = httpMock.expectOne(`${apiUrl}/pe-ttm`);
    expect(req.request.body).toEqual({
      marketData: { sharePrice: 150, eps: 10, bvps: null },
      fundamentalData: { marketCap: null, totalRevenue: null, epsGrowthRate: null },
      capitalStructure: { totalLiabilities: null, totalEquity: null, netIncome: null },
    });

    req.flush(15);

    expect(component.result()).toBe(15);
    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
  });

  it('converts the whole-percentage growth input to a fractional rate for a different metric', () => {
    component.selectMetric('pegRatio');
    component.playgroundForm.patchValue({ sharePrice: 200, eps: 10, epsGrowthRate: 20 });
    component.calculate();

    const req = httpMock.expectOne(`${apiUrl}/peg`);
    expect(req.request.body.fundamentalData.epsGrowthRate).toBe(0.2);

    req.flush(20);
  });

  it('surfaces the backend error and resets loading without touching result', () => {
    component.playgroundForm.patchValue({ sharePrice: 150, eps: 10 });
    component.calculate();

    const req = httpMock.expectOne(`${apiUrl}/pe-ttm`);
    req.flush('eps must be positive', { status: 422, statusText: 'Unprocessable Entity' });

    expect(component.loading()).toBe(false);
    expect(component.result()).toBeNull();
    expect(component.errorMessage()).toContain('Failed to calculate P/E (TTM)');
  });
});
