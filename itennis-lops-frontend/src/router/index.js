import Vue from 'vue'
import Router from 'vue-router'
import iTennis from '@/components/page/iTennis'
import Pxe from '@/components/page/Pxe'
import MainContent from '@/components/common/MainContent'

Vue.use(Router);

export default new Router({
  routes: [
    {
      path: '/',
      name: 'iTennis',
      component: iTennis,
      children:[
        {
          path: 'pxe',
          name: 'pxe',
          component: Pxe
        },
        {
          path: 'mainContent',
          name: 'mainContent',
          component: MainContent
        }
      ]
    }
  ]
})
