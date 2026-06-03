import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/login/index.vue'
import NotFound from '@/views/NotFound/index.vue';
import RegisterPage from '@/views/register/index.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home',
      name: 'Home',
      component: () => import('@/views/home/index.vue'),
      children: [
          { path: '/job', name: 'Job', component: () => import('@/views/home/components/JobPage.vue') },
          { path: 'job/:id', name: 'JobDetail', component: () => import('@/views/job/JobDetail.vue') },
          { path: '/resume', name: 'Resume', component: () => import('@/views/home/components/ResumePage.vue') },
          { path: '/resume/create', name: 'ResumeCreate', component: () => import('@/views/home/components/ResumeCreate.vue') },
          { path: '/my-invote', name: 'MyInvote', component: () => import('@/views/home/components/MyInvotePage.vue') },
          { path: '/company', name: 'HomeCompany', component: () => import('@/views/home/components/CompanyPage.vue') },
          { path: 'company/:id', name: 'CompanyDetail', component: () => import('@/views/company/CompanyDetail.vue') },
          { path: '/home', name: 'HomePage', component: () => import('@/views/home/components/HomePage.vue') },
      ]
    },
    {
      path: '/admin',
      name: 'SysAdmin',
      component: () => import('@/views/sysadmin/index.vue'),
      children: [
        {
          path: '',
          redirect: '/admin/certification-manage'
        },
        {
          path: 'certification-manage',
          name: 'SysAdminCertificationManage',
          component: () => import('@/views/sysadmin/components/CertificationManage.vue')
        },
        {
          path: 'company-manage',
          name: 'SysAdminCompanyManage',
          component: () => import('@/views/sysadmin/components/CompanyManage.vue')
        },
        {
          path: 'user-manage',
          name: 'SysAdminUserManage',
          component: () => import('@/views/sysadmin/components/UserManage.vue')
        },
        {
          path: 'job-manage',
          name: 'SysAdminJobManage',
          component: () => import('@/views/sysadmin/components/JobManage.vue')
        },
        {
          path: 'offer-manage',
          name: 'SysAdminOfferManage',
          component: () => import('@/views/sysadmin/components/OfferManage.vue')
        },
        {
          path: 'statistics',
          name: 'SysAdminStatistics',
          component: () => import('@/views/sysadmin/components/DataStatistic.vue')
        },
        {
          path: 'system-message',
          name: 'SysAdminSystemMessage',
          component: () => import('@/views/sysadmin/components/SystemMessagePush.vue')
        }
      ]
    },
    {
      path: '/recruiter',
      name: 'Recruiter',
      component: () => import('@/views/recruiter/index.vue'),
      children: [
        {
          path: 'job-publish',
          name: 'JobPublish',
          component: () => import('@/views/recruiter/components/JobPublish.vue')
        },
        {
          path: 'job-list',
          name: 'JobList',
          component: () => import('@/views/recruiter/components/JobList.vue')
        },
        {
          path: 'job/:id',
          name: 'RecruiterJobDetail',
          component: () => import('@/views/recruiter/components/JobDetail.vue')
        },
        {
          path: 'job/edit/:id',
          name: 'RecruiterJobEdit',
          component: () => import('@/views/recruiter/components/JobEdit.vue')
        },
        {
          path: 'application-records',
          name: 'ApplicationRecords',
          component: () => import('@/views/recruiter/components/ApplicationRecords.vue')
        },
        {
          path: 'interview-records',
          name: 'InterviewRecords',
          component: () => import('@/views/recruiter/components/InterviewRecords.vue')
        },
        {
          path: 'invite-interview',
          name: 'InviteInterview',
          component: () => import('@/views/recruiter/components/InviteInterview.vue')
        },
        {
          path: 'join-interview',
          name: 'RecruiterJoinInterview',
          component: () => import('@/views/recruiter/components/JoinInterview.vue')
        },
        {
          path: 'send-offer',
          name: 'SendOffer',
          component: () => import('@/views/recruiter/components/SendOffer.vue')
        },
        {
          path: 'bind-company',
          name: 'BindCompany',
          component: () => import('@/components/BindCompany.vue')
        },
        {
          path: 'unbind-company',
          name: 'UnbindCompany',
          component: ( ) => import('@/components/UnbindingCompany.vue')
        }
      ]
    },
    {
        path: '/comp',
        name: 'Company',
        component: () => import('@/views/compadmin/index.vue'),
        children: [
          { path: '/comp/info', name: 'CompanyInfo', component: () => import('@/views/compadmin/components/CompanyInfo.vue') },
          { path: '/company/edit/:id', name: 'CompanyEdit', component: () => import('@/views/compadmin/components/CompanyEdit.vue') },
          { path: '/comp/certification', name: 'CompanyCertification', component: () => import('@/views/compadmin/components/CompCertification.vue') },
          { path: '/comp/comp-certification', name: 'CompanyCertification', component: () => import('@/views/compadmin/components/CompCertification.vue') },
          { path: '/comp/user-certification', name: 'UserCertification', component: () => import('@/views/compadmin/components/UserCertification.vue') },
          { path: '/comp/user-manage', name: 'CompUserManage', component: () => import('@/views/compadmin/components/CompUserManage.vue') },
          { path: '/comp/statetistic' , name: 'CompanyStatistic', component: () => import('@/views/compadmin/components/DataStatistic.vue')},
          { path: '/comp/setting', name: 'CompanySetting', component: () => import('@/views/compadmin/components/CompSetting.vue')},
          {
            path: '/comp/unbind',
            name: 'CompAdminUnbind',
            component: () => import('@/components/UnbindingCompany.vue'),
          },
          {
            path: '/comp/bind',
            name: 'CompAdminBind',
            component: () => import('@/components/BindCompany.vue'),
          }
        ]
    },
    {
      path: '/register',
      name: 'Register',
      component: RegisterPage,
    },
    {
      path: '/login',
      name: 'Login',
      component: Login
    },
    {
      path: '/api-test',
      name: 'ApiTest',
      component: () => import('@/api/ApiTest.vue')
    },
    {
      path: '/meeting/trtc',
      name: 'TrtcIframeMeeting',
      component: () => import('@/views/meeting/TrtcIframeRoom.vue')
    },
    {
      path: '/meeting/mock-interview',
      name: 'MockInterview',
      component: () => import('@/views/meeting/MockInterview.vue')
    },
    {
      path: '/personal',
      name: 'Personal',
      component: () => import('@/views/personal/index.vue'),
      children: [
          { 
            path: '/personal/info/edit', 
            name: 'PersonalInfoEdit', 
            component: () => import('@/views/personal/components/PersonalInfoEdit.vue') 
          },
          {
            path: '/personal/my',
            name: 'PersonalMy',
            component: () => import('@/views/personal/components/PersonalMy.vue')
          },
          {
            path: '/personal/message',
            name: 'PersonalMessage',
            component: () => import('@/views/personal/components/PersonalMessage.vue')
          },
          {
            path: '/personal/interview',
            name: 'PersonalInterview',
            component: () => import('@/views/personal/components/InterviewNoticeList.vue')
          },
          {
            path: '/personal/settings',
            name: 'SystemSetting',
            component: () => import('@/views/personal/components/SystemSetting.vue')
          },
          {
            path: '/personal/help',
            name: 'HelpCenter',
            component: () => import('@/views/personal/components/HelpCenter.vue')
          }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: NotFound
    }
  ],
})

export default router
