<template>
    <div class="icon-manager">
        <!-- 页面标题区域 -->
        <div class="page-header">
            <h2>图标管理</h2>
            <el-tag type="info">共 {{ total }} 个图标</el-tag>
        </div>

        <div class="content-box">
            <!-- 操作栏 -->
            <el-card class="operation-area" shadow="hover">
                <!-- 搜索区域 -->
                <el-form ref="searchForm" :inline="true" :model="listQuery" class="search-form">
                    <el-form-item label="图标名称">
                        <el-input v-model="listQuery.itemKey" placeholder="请输入图标名称" clearable></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" plain @click="onSubmit">
                            <el-icon><Search /></el-icon> 查询
                        </el-button>
                        <el-button plain @click="onReset">
                            <el-icon><Refresh /></el-icon> 重置
                        </el-button>
                    </el-form-item>
                </el-form>

                <!-- 按钮区域 -->
                <div class="control-btns">
                    <el-button type="danger" plain size="default" @click="batchDelete" :disabled="!multipleSelection.length">
                        <el-icon><Delete /></el-icon> 批量删除
                    </el-button>
                    <div class="right-btns">
                        <el-button type="primary" plain size="default" @click="exportVisible = true">
                            <el-icon><Download /></el-icon> 导出数据
                        </el-button>
                        <el-button type="primary" plain size="default" @click="handleCreate">
                            <el-icon><Plus /></el-icon> 新增图标
                        </el-button>
                    </div>
                </div>
            </el-card>

            <!-- 表格区域 -->
            <el-card class="table-card" shadow="hover">
                <el-table ref="multipleTable" 
                         v-loading="listLoading" 
                         :data="tableData" 
                         tooltip-effect="dark" 
                         row-key="id"
                         style="width: 100%" 
                         size="default" 
                         @selection-change="handleSelectionChange"
                         border>
                    <el-table-column type="selection" width="50" align="center"></el-table-column>
                    <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
                    <el-table-column prop="itemKey" label="图标名称" min-width="120"></el-table-column>
                    <el-table-column prop="itemValue" label="图标值" min-width="120"></el-table-column>
                    <el-table-column label="图标预览" width="120" align="center">
                        <template #default="{ row }">
                            <div v-if="row.itemValue" class="icon-display">
                                <i :class="getIconClass(row.itemValue)" class="icon-preview"></i>
                            </div>
                            <span v-else class="no-icon">未设置</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="150"></el-table-column>
                    <el-table-column prop="createTime" label="创建时间" width="160" align="center"></el-table-column>
                    <el-table-column prop="updateTime" label="更新时间" width="160" align="center"></el-table-column>
                    <el-table-column label="操作" width="180" align="center" fixed="right">
                        <template #default="{ row }">
                            <el-button size="small" type="primary" plain @click="handleEdit(row)">
                                <el-icon><Edit /></el-icon> 编辑
                            </el-button>
                            <el-button size="small" type="danger" plain @click="handleDelete(row.id)">
                                <el-icon><Delete /></el-icon> 删除
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <!-- 分页 -->
                <pagination v-show="total > 0" 
                          :total="total" 
                          v-model:page="listQuery.currentPage" 
                          v-model:limit="listQuery.pageSize" 
                          @pagination="fetchData" />
            </el-card>

            <!-- 新增/编辑弹窗 -->
            <el-dialog :title="dialogForm.id ? '编辑图标' : '新增图标'" 
                      v-model="formVisible" 
                      width="500px"
                      :close-on-click-modal="false">
                <el-form :model="dialogForm" :rules="formRules" ref="dialogForm" label-width="100px">
                    <el-form-item label="图标名称" prop="itemKey">
                        <el-input v-model="dialogForm.itemKey" placeholder="请输入图标名称"></el-input>
                    </el-form-item>
                    <el-form-item label="图标值" prop="itemValue">
                        <el-input v-model="dialogForm.itemValue" placeholder="请输入图标值，如：iconfont icon-xxx 或 fas fa-xxx">
                            <template #append>
                                <div class="icon-preview-append">
                                    <i v-if="dialogForm.itemValue" :class="getIconClass(dialogForm.itemValue)" class="preview-icon"></i>
                                    <span v-else class="no-preview">预览</span>
                                </div>
                            </template>
                        </el-input>
                        <div v-if="dialogForm.itemValue" class="icon-preview-box">
                            <div class="preview-label">图标预览：</div>
                            <i :class="getIconClass(dialogForm.itemValue)" class="preview-icon-large"></i>
                        </div>
                    </el-form-item>
                    <el-form-item label="描述">
                        <el-input type="textarea" v-model="dialogForm.description" placeholder="请输入描述信息"></el-input>
                    </el-form-item>
                </el-form>
                <template #footer>
                    <div class="dialog-footer">
                        <el-button @click="formVisible = false" plain>取 消</el-button>
                        <el-button type="primary" :loading="isSubmit" @click="handleSave('dialogForm')" plain>确 定</el-button>
                    </div>
                </template>
            </el-dialog>

            <!-- 导出数据弹窗 -->
            <el-dialog title="导出数据" v-model="exportVisible" width="400px" :close-on-click-modal="false">
                <div class="export-data">
                    <el-button type="primary" plain @click="exportTable('xlsx')">
                        <i class="fas fa-file-excel"></i> EXCEL格式
                    </el-button>
                    <el-button type="primary" plain @click="exportTable('csv')">
                        <i class="fas fa-file-csv"></i> CSV格式
                    </el-button>
                    <el-button type="primary" plain @click="exportTable('txt')">
                        <i class="fas fa-file-alt"></i> TXT格式
                    </el-button>
                </div>
                <div class="hints">提示：请选择要导出数据的格式</div>
            </el-dialog>
        </div>
    </div>
</template>

<script>
import excel from '../utils/excel.js'
import logger from '@/utils/logger'
import Pagination from '../components/Pagination/index.vue'
import Upload from '../components/Upload/index.vue'
import Hints from '../components/Hints/index.vue'
import Request from '../utils/request.js'
import { Search, Refresh, Delete, Download, Plus, Edit } from '@element-plus/icons-vue'
import '@/assets/iconfont.css'
export default {
    name: 'IconDict',
    components: { 
      Pagination, 
      Upload, 
      Hints,
      Search,
      Refresh,
      Delete,
      Download,
      Plus,
      Edit
    },
    data() {
        return {
            // 数据列表加载动画
            listLoading: true,
            formLabelWidth: '80px',

            // 查询列表参数对象
            listQuery: {
                itemKey: undefined,
                currentPage: 1,
                pageSize: 10
            },
            // 新增/编辑提交表单对象
            dialogForm: {
                id: undefined, // 需要有id字段以区分新增和编辑
                dictTypeCode: 'icon', // 字典类型Code，这里假设是固定的
                itemKey: '',
                itemValue: '',
                description: '',
            },

            // 数据总条数
            total: 0,
            // 表格数据数组
            tableData: [],
            // 多选数据暂存数组
            multipleSelection: [],
            // 新增/编辑 弹出框显示/隐藏
            formVisible: false,
            // 表单校验 trigger: ['blur', 'change'] 为多个事件触发
            formRules: {
                itemKey: [
                    { required: true, message: '字典键不能为空', trigger: 'blur' },
                    { min: 1, max: 20, message: '字典键长度必须在1到20个字符之间', trigger: 'blur' }
                ],
                itemValue: [
                    { required: true, message: '字典值不能为空', trigger: 'blur' },
                    { min: 1, max: 50, message: '字典值长度必须在1到50个字符之间', trigger: 'blur' }
                ],

                description: [
                    { max: 100, message: '描述内容不能超过100个字符', trigger: 'blur' }
                ],

            },
            // 防止多次连续提交表单
            isSubmit: false,

            // 导出文件格式
            filesFormat: '.txt, .csv, .xls, .xlsx',
            // 导出数据 弹出框显示/隐藏
            exportVisible: false
        }
    },
    created() {
        this.fetchData()
    },
    methods: {
        // 多选操作
        handleSelectionChange(val) {
            this.multipleSelection = val.map(v => v.id);
            logger.debug('选中的数据:', this.multipleSelection);
        },
        // 新建数据
        handleCreate() {
            this.dialogForm = {
                dictTypeCode: 'icon',
                itemKey: '',
                itemValue: '',
                description: ''
            }
            this.formVisible = true
        },
        // 编辑数据
        handleEdit(row) {
            this.dialogForm = JSON.parse(JSON.stringify(row))
            this.formVisible = true
        },
        cancel() {
            this.$message.success('取消操作成功');
        },
        // 删除数据
        handleDelete(id) {
            this.$confirm('确认删除该图标?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(async () => {
                try {
                    const res = await Request.delete(`/dictitem/deleteById/${id}`);
                    if (res.code === '0') {
                        this.$message.success('删除成功');
                        this.fetchData();
                    } else {
                        this.$message.error(res.msg || '删除失败');
                    }
                } catch (error) {
                    logger.error('删除失败:', error);
                    this.$message.error('删除失败');
                }
            }).catch(() => {
                this.$message.info('已取消删除');
            });
        },

        // 批量删除
        batchDelete() {
            if (!this.multipleSelection.length) {
                this.$message.warning('请先选择要删除的数据！');
                return;
            }

            this.$confirm('确认删除选中的图标?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(async () => {
                try {
                    const ids = this.multipleSelection.join(',');
                    const res = await Request.delete(`/dictitem/deleteBatch?idList=${ids}`);
                    if (res.code === '0') {
                        this.$message.success('批量删除成功');
                        this.fetchData();
                    } else {
                        this.$message.error(res.msg || '批量删除失败');
                    }
                } catch (error) {
                    logger.error('批量删除失败:', error);
                    this.$message.error('批量删除失败');
                }
            }).catch(() => {
                this.$message.info('已取消删除');
            });
        },

        // 获取数据列表
        async fetchData() {
            this.listLoading = true;
            try {
                const res = await Request.get("/dictitem/findPage", {
                    params: {
                        code: "icon",
                        itemKey: this.listQuery.itemKey,
                        currentPage: this.listQuery.currentPage,
                        size: this.listQuery.pageSize,
                    }
                });
                
                if (res.code === '0') {
                    this.total = res.data.total;
                    this.tableData = res.data.records;
                    // 清空选择
                    this.multipleSelection = [];
                    if (this.$refs.multipleTable) {
                        this.$refs.multipleTable.clearSelection();
                    }
                }
            } catch (error) {
                logger.error('获取数据失败:', error);
                this.$message.error('获取数据失败');
            } finally {
                this.listLoading = false;
            }
        },
        // 查询数据
        onSubmit() {
            this.listQuery.currentPage = 1
            this.fetchData()
        },
        onReset() {
            this.listQuery.currentPage = 1
            this.listQuery.pageSize = 10;
            this.listQuery.itemKey = "";
            this.fetchData()
        },

        handleSave(formName) {
            this.$refs[formName].validate(async valid => {
                if (!valid) {
                    logger.debug('表单验证失败!')
                    return false
                }

                this.isSubmit = true
                try {
                    // 创建一个新对象，只包含需要的字段
                    const submitData = {
                        dictTypeCode: this.dialogForm.dictTypeCode,
                        itemKey: this.dialogForm.itemKey,
                        itemValue: this.dialogForm.itemValue,
                        description: this.dialogForm.description
                    }
                    
                    // 如果是编辑，添加 id
                    if (this.dialogForm.id) {
                        submitData.id = this.dialogForm.id
                    }

                    const method = submitData.id ? 'put' : 'post'
                    const url = submitData.id 
                        ? `/dictitem/${submitData.id}` 
                        : '/dictitem/save'
                    
                    const response = await Request[method](url, submitData)
                    
                    if (response.code === '0') {
                        this.$message.success(submitData.id ? '更新成功' : '添加成功')
                        this.formVisible = false
                        this.fetchData()
                    } else {
                        this.$message.error(response.msg || (submitData.id ? '更新失败' : '添加失败'))
                    }
                } catch (error) {
                    logger.error('保存失败:', error)
                    this.$message.error('操作失败')
                } finally {
                    this.isSubmit = false
                }
            })
        },

        // 导出数据--excel格式
        exportTable(type) {
            if (this.tableData.length) {
                const params = {
                    header: ['ID', '图标名称', '图标值', '描述', '创建时间', '更新时间'],
                    key: ['id', 'itemKey', 'itemValue', 'description', 'createTime', 'updateTime'],
                    data: this.tableData,
                    autoWidth: true,
                    fileName: '图标数据表',
                    bookType: type
                }
                excel.exportDataToExcel(params)
                this.exportVisible = false
            }
        },

        // 获取图标类名，确保正确显示
        getIconClass(iconValue) {
            if (!iconValue) {
                return ''
            }
            
            // 转换为字符串
            if (typeof iconValue !== 'string') {
                iconValue = String(iconValue)
            }
            
            // 去除首尾空格
            iconValue = iconValue.trim()
            
            // 如果已经包含 iconfont 或 Font Awesome 类，直接返回
            if (iconValue.includes('iconfont') || 
                iconValue.includes('fa-') || 
                iconValue.startsWith('fas ') || 
                iconValue.startsWith('far ') || 
                iconValue.startsWith('fal ') || 
                iconValue.startsWith('fab ')) {
                return iconValue
            }
            
            // 如果以 icon- 开头，添加 iconfont 类
            if (iconValue.startsWith('icon-')) {
                return `iconfont ${iconValue}`
            }
            
            // 如果以 fa- 开头但没有 fas/far/fal/fab 前缀，添加 fas 前缀
            if (iconValue.startsWith('fa-')) {
                return `fas ${iconValue}`
            }
            
            // 如果包含空格，可能是多个类名，尝试添加 iconfont 到最前面
            if (iconValue.includes(' ')) {
                // 检查是否已经有字体图标类
                const parts = iconValue.split(' ')
                const hasFontClass = parts.some(part => 
                    part === 'iconfont' || 
                    part === 'fas' || 
                    part === 'far' || 
                    part === 'fal' || 
                    part === 'fab'
                )
                if (!hasFontClass) {
                    return `iconfont ${iconValue}`
                }
                return iconValue
            }
            
            // 其他情况，尝试添加 iconfont
            return `iconfont ${iconValue}`
        }
    }
}
</script>

<style lang="less" scoped>
.icon-manager {
    padding: 24px;
    background-color: #f0f2f5;
    min-height: calc(100vh - 60px);
}

.page-header {
    display: flex;
    align-items: center;
    margin-bottom: 24px;

    h2 {
        font-size: 24px;
        font-weight: 500;
        color: #1f2f3d;
        margin: 0;
        margin-right: 15px;
    }
}

.operation-area {
    background: white;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    margin-bottom: 24px;
}

.search-form {
    margin-bottom: 20px;
    padding: 20px 20px 0;
    border-radius: 4px;
}

.table-card {
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.control-btns {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px 20px;

    .right-btns {
        display: flex;
        gap: 10px;
    }
}

:deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;

    th {
        background-color: #fafafa;
        font-weight: 500;
        color: #1f2f3d;
        padding: 12px 0;
    }

    td {
        padding: 12px 0;
    }
}

:deep(.el-dialog) {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

    .el-dialog__header {
        padding: 24px;
        border-bottom: 1px solid #ebeef5;
        margin: 0;
    }

    .el-dialog__title {
        font-size: 18px;
        font-weight: 500;
        color: #1f2f3d;
    }

    .el-dialog__body {
        padding: 32px 24px;
    }

    .el-dialog__footer {
        padding: 16px 24px;
        border-top: 1px solid #ebeef5;
    }
}

.export-data {
    display: flex;
    justify-content: space-around;
    margin-bottom: 20px;
}

.hints {
    text-align: center;
    color: #909399;
    font-size: 12px;
}

/* 图标显示样式 */
.icon-display {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 8px;
    min-height: 40px;
}

.icon-preview {
    font-size: 24px;
    color: #409eff;
    display: inline-block;
    line-height: 1;
}

.no-icon {
    color: #909399;
    font-size: 12px;
}

.icon-preview-append {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 40px;
    height: 100%;
}

.preview-icon {
    font-size: 18px;
    color: #409eff;
}

.no-preview {
    color: #909399;
    font-size: 12px;
}

.icon-preview-box {
    margin-top: 8px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    text-align: center;
}

.preview-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
}

.preview-icon-large {
    font-size: 32px;
    color: #409eff;
    display: inline-block;
}
</style>