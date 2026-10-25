import { createRouter, createWebHistory } from 'vue-router'
import { h } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
const home = () => import('../layout/index.vue')


const ShowView = () => import('@/views/showView.vue')

const Login = () => import('@/views/Login.vue')
const LRLayout = () => import('@/layout/LRLayout.vue')
const RegisterVue = () => import('@/views/RegisterVue.vue')
const Forget = () => import('@/views/Forget.vue')
const Menu = () => import('@/views/Menu.vue')
import Home from '@/views/front/Home.vue';

const routes = [

  {
    path: "/login",
    name: "LRLayout",
    redirect: '/login',  
    component:LRLayout,
    children: [
      {
        path: "/login",
        name: "Login",
        component: Login,
      },
      {
        path: "/register",
        name: "Register",
        component: RegisterVue
      },

      {
        path:'/forget',
        name: 'Forget',
        component: Forget
      },
 
    ]
  },
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/404.vue')
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('@/views/front/Products.vue')
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/front/Cart.vue')
  },
  {
    path: '/favorite',
    name: 'Favorite',
    component: () => import('@/views/front/Favorite.vue'),
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/category/:id',
    name: 'category',
    component: () => import('@/views/front/Category.vue')
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('@/views/front/Order.vue')
  },
  {
    path: '/user-center',
    name: 'UserCenter',
    component: () => import('@/views/front/UserCenter.vue'),
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/front/ProductDetail.vue')
  },
  {
    path: '/articles',
    name: 'Articles',
    component: () => import('@/views/front/Article.vue')
  },
  {
    path: '/recommend-center',
    name: 'RecommendCenter',
    component: () => import('@/views/front/RecommendCenter.vue'),
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/front/ArticleDetail.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/front/Search.vue'),
    meta: {
      title: '搜索结果'
    }
  },
  // 拼团相关路由
  {
    path: '/group-buy',
    name: 'GroupBuyList',
    component: () => import('@/views/front/GroupBuyList.vue'),
    meta: {
      title: '限时拼团'
    }
  },
  {
    path: '/group-buy/:id',
    name: 'GroupBuyDetail',
    component: () => import('@/views/front/GroupBuyDetail.vue'),
    meta: {
      title: '拼团详情'
    }
  },
  {
    path: '/my-group-buy',
    name: 'MyGroupBuy',
    component: () => import('@/views/front/MyGroupBuy.vue'),
    meta: {
      title: '我的拼团',
      requiresAuth: true
    }
  },
  {
    path: '/group-buy-team/:id',
    name: 'GroupBuyTeamDetail',
    component: () => import('@/views/front/GroupBuyTeamDetail.vue'),
    meta: {
      title: '拼团详情',
      requiresAuth: true
    }
  }
  
  
];



const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});
function createMenuRoute(item) {
  return {
    path: item.path.replace(/^\//, ''),
    name: item.name,
    component: () => import(`../views/${item.pagePath}.vue`).catch(err => {
      logger.error(`加载组件失败: ${item.pagePath}`, err)
      return {
        render() {
          return h('div', {
            style: { padding: '20px', textAlign: 'center', color: '#f56c6c', backgroundColor: '#fef0f0', border: '1px solid #fbc4c4', borderRadius: '4px', margin: '20px' }
          }, [
            h('h3', { style: { margin: '0 0 10px 0' } }, '组件加载失败'),
            h('p', { style: { margin: 0 } }, `无法加载组件: ${item.pagePath}`)
          ])
        }
      }
    }),
    meta: { title: item.name }
  }
}

function appendMenuRoutes(menus, children) {
  menus.forEach(item => {
    if (!item || typeof item !== 'object') return
    if (item.path) {
      if (!item.pagePath) {
        logger.warn(`菜单项 ${item.name} 缺少 pagePath 属性`)
        return
      }
      children.push(createMenuRoute(item))
    } else if (Array.isArray(item.children)) {
      appendMenuRoutes(item.children, children)
    }
  })
}

export const setRoutes = () => {
  const raw = localStorage.getItem('userMenuList')
  if (!raw || raw === 'undefined' || router.hasRoute('home')) return
  try {
    const menus = JSON.parse(raw)
    if (!Array.isArray(menus)) {
      logger.warn('userMenuList must be an array')
      return
    }
    const children = [
      { path: '/showView', name: 'homeView', component: ShowView, meta: { title: '首页' } },
      { path: '/menu', name: 'Menu', component: Menu, meta: { title: '菜单管理' } }
    ]
    appendMenuRoutes(menus, children)
    router.addRoute({ path: '/', name: 'home', component: home, redirect: '/showView', children })
  } catch (err) {
    logger.error('Failed to parse userMenuList:', err)
  }
}
setRoutes()
router.onError((error) => {
  logger.error('路由错误:', error);
  if (error.message.includes('Cannot find module') || error.message.includes('Failed to fetch dynamically imported module')) {
    // 如果错误是因为找不到模块或动态导入失败，跳转到404页面
    router.replace('/404');
  } else if (error.message.includes('Component is missing template')) {
    // 如果组件缺少模板，也跳转到404页面
    logger.error('组件加载失败，可能是文件结构问题:', error);
    router.replace('/404');
  }
});
router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    const userInfo = localStorage.getItem('frontUser')
    if (!userInfo) {
      ElMessage({
        message: '请先登录',
        type: 'warning'
      })
      next('/login')
      return
    }
  }

  if (to.name) {
    localStorage.setItem('currentPathName', to.name);
  }

  const localMenus = localStorage.getItem("userMenuList");
  if (!to.matched.length) {
    if (to.path !== '/404') {
      if (localMenus) {
        // 记录来源路径
        next({
          path: '/404',
          replace: true,
          query: { redirect: from.fullPath }
        })
      } else {
        ElMessage({
          message: '请先登录',
          type: 'warning'
        });
        next('/login')
      }
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router;
