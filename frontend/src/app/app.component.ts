import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { HeaderComponent } from './components/header/header.component';
import { MetricDashboardComponent } from './components/metric-dashboard/metric-dashboard.component';
import { MetricExplorerComponent } from './components/metric-explorer/metric-explorer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatIconModule,
    HeaderComponent,
    MetricDashboardComponent,
    MetricExplorerComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class App {
  title = 'fa-metrics-frontend';
}
