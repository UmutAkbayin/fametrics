import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit {
  isDarkMode = signal(true);

  ngOnInit() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    this.isDarkMode.set(savedTheme === 'dark');
    this.applyTheme(this.isDarkMode());
  }

  toggleTheme() {
    this.isDarkMode.update((v) => !v);
    localStorage.setItem('theme', this.isDarkMode() ? 'dark' : 'light');
    this.applyTheme(this.isDarkMode());
  }

  private applyTheme(isDark: boolean) {
    const htmlEl = document.documentElement;
    if (isDark) {
      htmlEl.classList.remove('light-mode');
    } else {
      htmlEl.classList.add('light-mode');
    }
  }
}
