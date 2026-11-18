import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { ActualitesComponent } from './pages/actualites/actualites.component';
import { ArticleDetailComponent } from './pages/article-detail/article-detail.component';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { MonCompteComponent } from './pages/mon-compte/mon-compte.component';
import { roleGuard } from './core/guards/role.guard';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'actualites', component: ActualitesComponent },
  { path: 'actualites/:id', component: ArticleDetailComponent },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [roleGuard] },
  { path: 'mon-compte', component: MonCompteComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
