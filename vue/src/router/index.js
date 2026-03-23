import { createRouter, createWebHistory } from 'vue-router'
import { h } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import home from '../layout/index.vue';


import ShowView from '@/views/showView.vue';

import Login from '@/views/Login.vue';
import LRLayout from '@/layout/LRLayout.vue';
import RegisterVue from '@/views/RegisterVue.vue';
import Forget from '@/views/Forget.vue';
import Menu from '@/views/Menu.vue';
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
export const setRoutes = () => {
  const userMenuListStr = localStorage.getItem("userMenuList")
  if (userMenuListStr && userMenuListStr !== 'undefined') {
    const currentRoutes = router.getRoutes().map(v => v.name)
    if (!currentRoutes.includes('Layout')) {
      const currRouter = {
        path: '/',
        name: 'home',
        component: home,
        redirect: '/showView',
        children: [
          {
            path: "/showView",
            name: "homeView",
            component: ShowView,
            meta: {
              title: "首页"
            },
          },
          {
            path: '/menu',
            name: 'Menu',
            component: Menu,
            meta: {
              title: "菜单管理"
            },
          },
        ]
      }

      try {
      const menus = JSON.parse(userMenuListStr);
      menus.forEach((item) => {
        if (item.path) {
          if (!item.pagePath) {
            logger.warn(`菜单项 ${item.name} 缺少 pagePath 属性`);
            return;
          }
          const itemMenu = {
            path: item.path.replace("/", ""),
            name: item.name,
            component: () => import(`../views/${item.pagePath}.vue`).catch(err => {
              logger.error(`加载组件失败: ${item.pagePath}`, err);
              // 返回一个占位组件，避免空白页面（使用 render 函数，因为 Vue 3 运行时构建不支持 template）
              return {
                default: {
                  render() {
                    return h('div', {
                      style: { 
                        padding: '20px', 
                        textAlign: 'center',
                        color: '#f56c6c',
                        backgroundColor: '#fef0f0',
                        border: '1px solid #fbc4c4',
                        borderRadius: '4px',
                        margin: '20px'
                      }
                    }, [
                      h('h3', { style: { margin: '0 0 10px 0' } }, '组件加载失败'),
                      h('p', { style: { margin: 0 } }, `无法加载组件: ${item.pagePath}`)
                    ]);
                  }
                }
              };
            }),
            meta: {
              title: item.name
            }
          };
          currRouter.children.push(itemMenu);
        } else if (item.children?.length) {
          item.children.forEach((subItem) => {
            if (subItem.path) {
              if (!subItem.pagePath) {
                logger.warn(`菜单项 ${subItem.name} 缺少 pagePath 属性`);
                return;
              }
              const itemMenu = {
                path: subItem.path.replace("/", ""),
                name: subItem.name,
                component: () => import(`../views/${subItem.pagePath}.vue`).catch(err => {
                  logger.error(`加载组件失败: ${subItem.pagePath}`, err);
                  // 返回一个占位组件，避免空白页面（使用 render 函数，因为 Vue 3 运行时构建不支持 template）
                  return {
                    default: {
                      render() {
                        return h('div', {
                          style: { 
                            padding: '20px', 
                            textAlign: 'center',
                            color: '#f56c6c',
                            backgroundColor: '#fef0f0',
                            border: '1px solid #fbc4c4',
                            borderRadius: '4px',
                            margin: '20px'
                          }
                        }, [
                          h('h3', { style: { margin: '0 0 10px 0' } }, '组件加载失败'),
                          h('p', { style: { margin: 0 } }, `无法加载组件: ${subItem.pagePath}`)
                        ]);
                      }
                    }
                  };
                }),
                meta: {
                  title: subItem.name
                }
              };
              currRouter.children.push(itemMenu);
            }
          });
        }
      });
      router.addRoute(currRouter)
      } catch (err) {
        logger.error('Failed to parse userMenuList:', err);
    }
  }
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
