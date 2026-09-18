import { Component, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { HeaderComponent } from './components/header/header.component';
import { MetricDashboardComponent } from './components/metric-dashboard/metric-dashboard.component';
import { MetricExplorerComponent } from './components/metric-explorer/metric-explorer.component';
import { TopCandidatesComponent } from './components/top-candidates/top-candidates.component';
import { TopCandidateResponse } from './services/stock-valuation.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatIconModule,
    HeaderComponent,
    MetricDashboardComponent,
    MetricExplorerComponent,
    TopCandidatesComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class App {
  title = 'fa-metrics-frontend';
  selectedTabIndex = signal<number>(0);

  dashboard = viewChild(MetricDashboardComponent);

  onOpenCandidateInWorkspace(candidate: TopCandidateResponse): void {
    this.dashboard()?.prefillFromCandidate(candidate);
    this.selectedTabIndex.set(0);
  }
}
