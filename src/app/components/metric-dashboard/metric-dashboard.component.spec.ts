import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { MetricDashboardComponent } from './metric-dashboard.component';
import { MetricResponse, SummaryResponse } from '../../services/stock-valuation.service';

describe('MetricDashboardComponent', () => {
  let component: MetricDashboardComponent;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(MetricDashboardComponent);
    component = fixture.componentInstance;
    component.ngOnInit();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getMissingInputs', () => {
    it('lists every required field as missing on an empty form', () => {
      expect(component.getMissingInputs('PE_TTM')).toEqual(['Price', 'EPS']);
    });

    it('drops a field from the list once it has a valid value', () => {
      component.inputForm.patchValue({ sharePrice: 150 });

      expect(component.getMissingInputs('PE_TTM')).toEqual(['EPS']);
    });

    it('still counts a field as missing when its value fails validation', () => {
      // totalLiabilities requires >= 0.
      component.inputForm.patchValue({ totalLiabilities: -5, totalEquity: 80000000 });

      expect(component.getMissingInputs('DE_RATIO')).toEqual(['Liab.']);
    });

    it('returns an empty list once every required field is valid', () => {
      component.inputForm.patchValue({ sharePrice: 150, eps: 10 });

      expect(component.getMissingInputs('PE_TTM')).toEqual([]);
    });

    it('returns an empty list for a metric id it does not recognize', () => {
      expect(component.getMissingInputs('NOT_A_REAL_METRIC' as any)).toEqual([]);
    });
  });

  describe('getMetricResult', () => {
    it('returns null before any results have been calculated', () => {
      expect(component.getMetricResult('PE_TTM')).toBeNull();
    });

    it('finds the response matching the requested metric', () => {
      const peTtm: MetricResponse = { metric: 'PE_TTM', value: 15 };
      const pbRatio: MetricResponse = { metric: 'PB_RATIO', value: 3 };
      const results: SummaryResponse = { metrics: [peTtm, pbRatio] };
      component.results.set(results);

      expect(component.getMetricResult('PB_RATIO')).toEqual(pbRatio);
    });

    it('returns null when the backend omitted that metric (uncomputable from the given inputs)', () => {
      component.results.set({ metrics: [{ metric: 'PE_TTM', value: 15 }] });

      expect(component.getMetricResult('ROE')).toBeNull();
    });
  });
});
