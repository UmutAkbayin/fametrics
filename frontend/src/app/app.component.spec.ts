import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { describe, expect, it } from 'vitest';
import { App } from './app.component';

describe('App', () => {
  it('creates the application and initializes tabs', () => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations()
      ]
    });

    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
    expect(app.selectedTabIndex()).toBe(0);
  });

  it('switches to Valuation Workspace when a candidate is opened in workspace', () => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations()
      ]
    });

    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();

    app.selectedTabIndex.set(1);
    expect(app.selectedTabIndex()).toBe(1);

    app.onOpenCandidateInWorkspace({
      cik: 320193,
      name: 'Apple Inc.',
      ticker: 'AAPL',
      periodEnd: '2024-09-28',
      price: 220.5,
      qualityScore: 90,
      valueScore: { composite: 80 },
      finalScore: 85,
      metrics: []
    });

    expect(app.selectedTabIndex()).toBe(0);
  });
});
