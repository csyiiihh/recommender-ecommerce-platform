<template>
    <div class="aside-container">
        <el-scrollbar class="menu-scrollbar" :native="false">
            <el-menu
                :default-active="$route.path"
                class="el-menu-vertical"
                router
                :collapse-transition="false"
                background-color="#fff"
                text-color="#606266">

                <div v-for="item in userMenuList" :key="item.id">
                    <div v-if="item.path">
                        <div v-if="!item.pid">
                            <el-menu-item :index="item.path">
                                <i :class="getIconClass(item.icon)"></i>
                                <span>{{ getMenuName(item.name) }}</span>
                            </el-menu-item>
                        </div>
                    </div>
                    <div v-else>
                        <el-sub-menu :index="item.id + ''">
                            <template #title>
                                <i :class="getIconClass(item.icon)"></i>
                                <span>{{ getMenuName(item.name) }}</span>
                            </template>
                            <div v-for="subItem in item.children" :key="subItem.id">
                                <el-menu-item :index="subItem.path">
                                    <i :class="getIconClass(subItem.icon)"></i>
                                    <span>{{ getMenuName(subItem.name) }}</span>
                                </el-menu-item>
                            </div>
                        </el-sub-menu>
                    </div>
                </div>
            </el-menu>
        </el-scrollbar>
    </div>
</template>

<script>
import '../assets/iconfont.css';
import logger from '@/utils/logger'
import {setRoutes} from "@/router";
import Request from '../utils/request.js'
export default {
    name: 'Aside',
    components: {},
    // 用户信息
    userInfo: {},
    created() {
        let userMenuListStr = localStorage.getItem("userMenuList")
        this.userMenuList = userMenuListStr ? JSON.parse(userMenuListStr) : [];
        this.userInfo = localStorage.getItem("backUser") ? JSON.parse(localStorage.getItem("backUser")) : {};
        logger.debug("userMenuList:" + userMenuListStr);
    },
    data() {
        return {
          
            userMenuList: [
                {
                    name: 'Dashboard',
                    path: 'dashboard',
                    icon: 'el-icon-menu'
                },
                {
                    name: 'Reports',
                    path: 'reports',
                    icon: 'el-icon-document',
                    children: [
                        {
                            name: 'Report 1',
                            path: 'report1',
                            icon: 'el-icon-document'
                        },
                        {
                            name: 'Report 2',
                            path: 'report2',
                            icon: 'el-icon-document'
                        }
                    ]
                },
                // 更多静态菜单项...
            ],
            user: {},
            path: this.$route.path
        }
    },
    methods: {
        printMenu(menu) {
            logger.debug("Submenu:" + menu)
        },
        refreshMenu(){
            Request.get("/menu/getMenuTree/"+this.userInfo.id).then(response => {
                if (response.code === '0') {
                    localStorage.setItem("userMenuList", JSON.stringify(response.data))
                    setRoutes();
         
                } else {
                    this.$message({
                        type: 'error',
                        message: response.msg
                    })
                }
            })
        },
        getIconClass(icon) {
            if (!icon) return 'fas fa-circle'
            // 如果已经是 FontAwesome 图标，直接返回
            if (icon.startsWith('fas ') || icon.startsWith('far ') || icon.startsWith('fab ')) {
                return icon
            }
            // 如果是 Element Plus 图标组件，返回默认图标
            if (icon.includes('/')) {
                return 'fas fa-circle'
            }
            // 映射 el-icon-xxx 到 FontAwesome
            const iconMap = {
                'el-icon-menu': 'fas fa-bars',
                'el-icon-s-data': 'fas fa-chart-line',
                'el-icon-user': 'fas fa-user',
                'el-icon-info': 'fas fa-info-circle',
                'el-icon-lock': 'fas fa-lock',
                'el-icon-shopping-bag-1': 'fas fa-shopping-bag',
                'el-icon-shopping-cart-full': 'fas fa-shopping-cart',
                'el-icon-folder': 'fas fa-folder',
                'el-icon-shopping-cart-2': 'fas fa-shopping-cart',
                'el-icon-document': 'fas fa-file-alt',
                'el-icon-shopping-cart-1': 'fas fa-shopping-cart',
                'el-icon-chat-dot-square': 'fas fa-comments',
                'el-icon-setting': 'fas fa-cog',
                'el-icon-bell': 'fas fa-bell',
                'el-icon-picture': 'fas fa-image',
                'el-icon-s-tools': 'fas fa-tools',
                'el-icon-picture-outline-round': 'fas fa-image',
                'el-icon-service': 'fas fa-headset',
                'el-icon-chat-line-round': 'fas fa-comment-dots',
                'el-icon-tableware': 'fas fa-utensils',
                'el-icon-more': 'fas fa-ellipsis-h',
                'el-icon-check': 'fas fa-check',
                'el-icon-edit': 'fas fa-edit',
                'el-icon-delete': 'fas fa-trash',
                'el-icon-plus': 'fas fa-plus',
                'el-icon-refresh': 'fas fa-sync',
                'el-icon-search': 'fas fa-search',
                'el-icon-view': 'fas fa-eye',
                'el-icon-upload': 'fas fa-upload',
                'el-icon-download': 'fas fa-download'
            }
            return iconMap[icon] || 'fas fa-circle'
        },
        getMenuName(name) {
            // 菜单名称转换
            if (name === '农产品列表') {
                return '商品列表'
            }
            return name
        }
    }
}
</script>

<style lang="less" scoped>
.aside-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding-top: 8px;
    width: 100%; /* 确保宽度占满侧边栏 */
}

.menu-scrollbar {
    height: 100%;
    width: 100%;
}

/* 隐藏滚动条但保留滚动功能 */
.menu-scrollbar {
    :deep(.el-scrollbar__bar) {
        opacity: 0 !important;
    }
    
    :deep(.el-scrollbar__wrap) {
        overflow-x: hidden;
    }
}

.el-menu {
    border-right: none;
    padding: 12px;
    width: 100%;
    box-sizing: border-box;
}

.el-menu-item {
    height: 52px;
    line-height: 52px;
    border-radius: 10px;
    margin-bottom: 6px;
    color: #606266;
    font-size: 16px;
    text-align: left;
    padding: 0 20px !important;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    background: rgba(255, 255, 255, 0.5);
    border: 1px solid transparent;
    
    &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 0;
        background: linear-gradient(135deg, #3A5BA0, #4A6FB8);
        border-radius: 0 4px 4px 0;
        transition: height 0.4s cubic-bezier(0.4, 0, 0.2, 1);
        box-shadow: 0 2px 4px rgba(58, 91, 160, 0.2);
    }
    
    &:hover {
        color: #3A5BA0;
        background: linear-gradient(90deg, rgba(58, 91, 160, 0.12), rgba(58, 91, 160, 0.08)) !important;
        transform: translateX(6px);
        border-color: rgba(58, 91, 160, 0.2);
        box-shadow: 0 4px 12px rgba(58, 91, 160, 0.1);
        
        &::before {
            height: 70%;
        }
        
        i {
            transform: scale(1.15) rotate(5deg);
            color: #3A5BA0;
        }
    }
    
    &.is-active {
        color: #3A5BA0;
        background: linear-gradient(90deg, rgba(58, 91, 160, 0.18), rgba(58, 91, 160, 0.12)) !important;
        font-weight: 600;
        box-shadow: 
            0 4px 16px rgba(58, 91, 160, 0.2),
            0 2px 4px rgba(0, 0, 0, 0.05);
        border-color: rgba(58, 91, 160, 0.3);
        
        &::before {
            height: 80%;
            width: 5px;
        }
        
        i {
            color: #3A5BA0;
            transform: scale(1.2);
            filter: drop-shadow(0 2px 4px rgba(58, 91, 160, 0.3));
        }
    }
}

.el-sub-menu {
    :deep(.el-sub-menu__title) {
        height: 50px;
        line-height: 50px;
        border-radius: 8px;
        color: #606266;
        font-size: 16px;
        text-align: left;
        padding: 0 20px !important;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        
        &:hover {
            color: #3A5BA0;
            background: linear-gradient(90deg, rgba(58, 91, 160, 0.08), rgba(58, 91, 160, 0.05)) !important;
            
            i {
                transform: scale(1.1);
                color: #3A5BA0;
            }
        }
    }
}

.el-sub-menu {
    :deep(.el-menu--inline) {
        padding-left: 0;
        
        .el-menu-item {
            height: 45px;
            line-height: 45px;
            padding-left: 48px !important;
            background-color: transparent;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            
            &::before {
                content: '';
                position: absolute;
                left: 40px;
                top: 50%;
                transform: translateY(-50%);
                width: 4px;
                height: 0;
                background: linear-gradient(135deg, #3A5BA0, #4A6FB8);
                border-radius: 2px;
                transition: height 0.3s ease;
            }
            
            &:hover {
                color: #3A5BA0;
                background: linear-gradient(90deg, rgba(58, 91, 160, 0.08), rgba(58, 91, 160, 0.05)) !important;
                transform: translateX(4px);
                
                &::before {
                    height: 50%;
                }
                
                i {
                    transform: scale(1.1);
                    color: #3A5BA0;
                }
            }
            
            &.is-active {
                color: #3A5BA0;
                background: linear-gradient(90deg, rgba(58, 91, 160, 0.12), rgba(58, 91, 160, 0.08)) !important;
                font-weight: 600;
                box-shadow: 0 2px 8px rgba(58, 91, 160, 0.15);
                
                &::before {
                    height: 60%;
                }
                
                i {
                    color: #3A5BA0;
                    transform: scale(1.15);
                }
            }
        }
    }
}

/* 图标样式 */
.el-menu-item i,
.el-sub-menu i {
    font-size: 18px;
    margin-right: 12px;
    color: #909399;
    vertical-align: middle;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: inline-block;
}

.el-menu-item.is-active i,
.el-sub-menu.is-active i {
    color: #3A5BA0;
}

.el-menu-item:hover i {
    color: #3A5BA0;
}

.el-menu-item span {
    display: inline-block;
    vertical-align: middle;
}

.el-sub-menu {
    :deep(.el-sub-menu__title span) {
        display: inline-block;
        vertical-align: middle;
    }
}
</style>