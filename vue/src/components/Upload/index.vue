<template>
    <el-upload :headers="authenticatedHeaders"
      class="files-upload"
      :action="url"
      :accept="filesFormat"
      :multiple="isMultiple"
      :show-file-list="isShowList"
      :on-success="uploadSuccess"
      :on-error="uploadError"
    >
      <slot />
    </el-upload>
  </template>
  
  <script>
import { getAuthHeaders } from '@/utils/request'
export default {
    computed: { authenticatedHeaders() { return getAuthHeaders(); } },
    name: 'UploadView',
    props: {
      url: {
        type: String,
        default: ''
      },
      filesFormat: {
        type: String,
        default: ''
      },
      isMultiple: {
        type: Boolean,
        default: false
      },
      isShowList: {
        type: Boolean,
        default: false
      }
    },
    methods: {
      uploadSuccess(res, _file) {
        if (['200', '0'].includes(String(res.code))) {
          const data = res.data
          this.$emit('on-success', data)
        } else {
          this.$emit('on-error')
          this.$message.error(res.msg)
        }
      },
      uploadError(_res, _file) {
        this.$emit('on-error')
      }
    }
  }
  </script>
  
  <style lang="less">
  .files-upload{
    display: inline-block;
    vertical-align: top;
  }
  </style>
  