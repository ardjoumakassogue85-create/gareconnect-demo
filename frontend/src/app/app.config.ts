import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { ReservationService } from './core/services/reservation.service';
import { ReservationApiService } from './core/services/reservation-api.service';
import { GareService } from './core/services/gare.service';
import { GareApiService } from './core/services/gare-api.service';
import { ReclamationService } from './core/services/reclamation.service';
import { ReclamationApiService } from './core/services/reclamation-api.service';
import { VitrineService } from './core/services/vitrine.service';
import { VitrineApiService } from './core/services/vitrine-api.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled' })),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: ReservationService, useClass: ReservationApiService },
    { provide: GareService, useClass: GareApiService },
    { provide: ReclamationService, useClass: ReclamationApiService },
    { provide: VitrineService, useClass: VitrineApiService },
  ],
};
