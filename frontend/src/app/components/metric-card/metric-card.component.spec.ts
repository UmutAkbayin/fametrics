import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { MetricCardComponent } from './metric-card.component';
import { MetricResponse } from '../../services/stock-valuation.service';

describe('MetricCardComponent', () => {
  function createComponent() {
    const fixture = TestBed.createComponent(MetricCardComponent);
    fixture.componentRef.setInput('title', 'Price-to-Earnings Ratio');
    fixture.componentRef.setInput('abbreviation', 'P/E (TTM)');
    fixture.componentRef.setInput('formula', 'Price / EPS');
    fixture.componentRef.setInput('metricId', 'PE_TTM');
    return fixture;
  }

  it('shows a "Missing Inputs" state when there is no metric result', () => {
    const fixture = createComponent();
    fixture.componentRef.setInput('requiredInputs', ['sharePrice', 'eps']);

    const state = fixture.componentInstance.displayState();

    expect(state.label).toBe('Missing Inputs');
    expect(state.statusClass).toBe('neutral');
    expect(state.percentage).toBe(0);
    expect(state.hint).toContain('sharePrice, eps');
  });

  it('maps a FAVORABLE rating to the positive status and an 85% gauge', () => {
    const fixture = createComponent();
    const result: MetricResponse = {
      metric: 'PE_TTM',
      value: 12.5,
      assessment: { rating: 'FAVORABLE', label: 'Undervalued' },
    };
    fixture.componentRef.setInput('metricResult', result);

    const state = fixture.componentInstance.displayState();

    expect(state.label).toBe('Undervalued');
    expect(state.statusClass).toBe('positive');
    expect(state.percentage).toBe(85);
  });

  it('maps an UNFAVORABLE rating to the negative status and a 25% gauge', () => {
    const fixture = createComponent();
    const result: MetricResponse = {
      metric: 'PE_TTM',
      value: 40,
      assessment: { rating: 'UNFAVORABLE', label: 'Overvalued' },
    };
    fixture.componentRef.setInput('metricResult', result);

    const state = fixture.componentInstance.displayState();

    expect(state.statusClass).toBe('negative');
    expect(state.percentage).toBe(25);
  });
});
