import { Routes } from '@angular/router';

import { Login } from './features/auth/login/login';
import { AppShell } from './layout/app-shell/app-shell';
import { PatientRegistration } from './features/patients/registration/patient-registration/patient-registration';
import { PatientDetails } from './features/patients/details/patient-details/patient-details';
import { PatientEdit } from './features/patients/edit/patient-edit/patient-edit';
import { PatientSummary } from './features/patients/summary/patient-summary/patient-summary';
import { Placeholder } from './features/placeholder/placeholder';

export const routes: Routes = [

  {

    path: '',

    redirectTo: '/login',

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

        path: 'patients',

        redirectTo: 'patients/registration',

        pathMatch: 'full'

      },

      {

        path: 'patients/registration',

        component: PatientRegistration

      },

      {

        path: 'patients/details',

        component: PatientDetails

      },

      {

        path: 'patients/summary',

        component: PatientSummary

      },

      {

        path: 'patients/:patientId/edit',

        component: PatientEdit

      },

      {

        path: 'tab-2',

        component: Placeholder

      },

      {

        path: 'tab-3',

        component: Placeholder

      },

      {

        path: 'tab-4',

        component: Placeholder

      },

      {

        path: 'tab-5',

        component: Placeholder

      }

    ]

  },

  {

    path: '**',

    redirectTo: 'login'

  }

];
