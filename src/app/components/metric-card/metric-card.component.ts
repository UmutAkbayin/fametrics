import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MetricResponse, Rating } from '../../services/stock-valuation.service';

export interface CardDisplayState {
  label: string;
  statusClass: 'positive' | 'negative' | 'warning' | 'neutral';
  percentage: number;
  hint: string;
}

@Component({
  selector: 'app-metric-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTooltipModule,
    MatIconModule,
    MatProgressBarModule
  ],
  templateUrl: './metric-card.component.html',
  styleUrls: ['./metric-card.component.scss']
})
export class MetricCardComponent {
  title = input.required<string>();
  abbreviation = input.required<string>();
  formula = input.required<string>();
  metricResult = input<MetricResponse | null>(null);
  loading = input(false);
  metricId = input.required<string>();
  requiredInputs = input<string[]>([]);
  missingInputs = input<string[]>([]);
  sharePrice = input<number | null>(null);

  hasValue = computed(() => {
    const result = this.metricResult();
    return result !== undefined && result !== null && typeof result.value === 'number';
  });

  displayState = computed<CardDisplayState>(() => {
    const result = this.metricResult();

    if (!this.hasValue() || !result) {
      return {
        label: 'Missing Inputs',
        statusClass: 'neutral',
        percentage: 0,
        hint: `Requires inputs: ${this.requiredInputs().join(', ')}`
      };
    }

    const rating: Rating = result.assessment?.rating || 'NEUTRAL';
    const label = result.assessment?.label || 'Calculated';
    const hint = result.interpretation || result.description || '';

    let statusClass: 'positive' | 'negative' | 'warning' | 'neutral' = 'neutral';
    let percentage = 50;

    switch (rating) {
      case 'FAVORABLE':
        statusClass = 'positive';
        percentage = 85;
        break;
      case 'UNFAVORABLE':
        statusClass = 'negative';
        percentage = 25;
        break;
      case 'NEUTRAL':
        statusClass = 'warning';
        percentage = 60;
        break;
      case 'NOT_MEANINGFUL':
      default:
        statusClass = 'neutral';
        percentage = 50;
        break;
    }

    return {
      label,
      statusClass,
      percentage,
      hint
    };
  });
}

