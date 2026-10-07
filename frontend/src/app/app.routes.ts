import { Routes } from '@angular/router';

import { Login } from './features/auth/login/login';
import { AppShell } from './layout/app-shell/app-shell';
import { Placeholder } from './features/placeholder/placeholder';

export const routes: Routes = [

  {

    path: '',

    redirectTo: 'login',

    pathMatch: 'full'

  },

  {

    path: 'login',

    component: Login

  },

  {

    path: '',

    component: AppShell,

    children: [

      {

        path: 'home',

        component: Placeholder

      },

      {

        path: 'registrations',

        redirectTo: 'registrations/new',

        pathMatch: 'full'

      },

      {

        path: 'registrations/new',

        loadComponent: () => import('./features/registrations/new-registration/new-registration')
          .then(module => module.NewRegistration)

      },

      {

        path: 'registrations/search',

        loadComponent: () => import('./features/registrations/registration-search/registration-search')
          .then(module => module.RegistrationSearch)

      },

      {

        path: 'registrations/:registrationNo/bill',

        loadComponent: () => import('./features/registrations/bill/bill')
          .then(module => module.Bill)

      },

      {

        path: 'registrations/:registrationNo',

        loadComponent: () => import('./features/registrations/registration-details/registration-details')
          .then(module => module.RegistrationDetails)

      },

      {

        path: 'tests/search',

        loadComponent: () => import('./features/tests/tests-search/tests-search')
          .then(module => module.TestsSearch)

      },

      {

        path: 'tests/master',

        loadComponent: () => import('./features/tests/test-master/test-master')
          .then(module => module.TestMaster)

      },

      {

        path: 'payments/search',

        loadComponent: () => import('./features/payments/payments-search/payments-search')
          .then(module => module.PaymentsSearch)

      }

    ]

  },

  {

    path: '**',

    redirectTo: 'login'

  }

];
