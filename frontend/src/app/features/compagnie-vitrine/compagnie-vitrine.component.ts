import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { Trajet, VitrineCompagnie } from '../../core/models/metier.model';
import { VitrineService } from '../../core/services/vitrine.service';

@Component({
  selector: 'app-compagnie-vitrine',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './compagnie-vitrine.component.html',
  styleUrl: './compagnie-vitrine.component.scss',
})
export class CompagnieVitrineComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly vitrineService = inject(VitrineService);

  readonly compagnie = this.route.snapshot.paramMap.get('compagnie') ?? 'Compagnie';
  readonly vitrine = signal<VitrineCompagnie | null>(null);
  readonly trajets = signal<Trajet[]>([]);

  ngOnInit(): void {
    this.vitrineService.obtenirVitrine(this.compagnie).subscribe((vitrine) => this.vitrine.set(vitrine));
this.vitrineService.listerTrajetsPublics(this.compagnie).subscribe((trajets) => this.trajets.set(trajets));
  }
}
