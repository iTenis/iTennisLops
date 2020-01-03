<template>
  <div class="iTennis" id="iTennis">
    <el-container>
      <el-container>
        <el-aside width="200px" id="itaside">
          <el-menu :default-openeds="['2']">
            <div style="height: 60px;line-height: 60px">iTennis 柠檬
            </div>
            <el-submenu index="1">
              <template slot="title"><i class="el-icon-s-management"></i>状态面板</template>
              <el-menu-item index="1-1">
                <router-link :to="{name:'mainContent'}" tag="li">监控信息</router-link>
              </el-menu-item>
            </el-submenu>
            <el-submenu index="2">
              <template slot="title"><i class="el-icon-menu"></i>高级功能</template>
              <el-menu-item index="2-1">
                <router-link :to="{name:'mainContent'}" tag="li">运维集成平台</router-link>
              </el-menu-item>
              <el-menu-item index="2-2">
                <router-link :to="{name:'pxe'}" tag="li">定时任务调度</router-link>
              </el-menu-item>
            </el-submenu>
            <el-submenu index="3">
              <template slot="title"><i class="el-icon-message"></i>用户中心</template>
              <el-menu-item index="3-1">
                <router-link :to="{name:'pxe'}" tag="li">用户信息管理</router-link>
              </el-menu-item>
            </el-submenu>
            <router-link :to="{name:'pxe'}"></router-link>
            <el-submenu index="4">
              <template slot="title"><i class="el-icon-setting"></i>系统配置</template>
              <el-menu-item index="4-1">
                <router-link :to="{name:'pxe'}" tag="li">授权注册</router-link>
              </el-menu-item>
              <el-menu-item index="4-2">
                <router-link :to="{name:'pxe'}" tag="li">关于</router-link>
              </el-menu-item>
            </el-submenu>
          </el-menu>
        </el-aside>

        <el-container>

          <el-header style="text-align: right; font-size: 12px; ">

            <el-dropdown>
              <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png">iTennis</el-avatar>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item>个人信息</el-dropdown-item>
                <el-dropdown-item>修改密码</el-dropdown-item>
                <el-dropdown-item>授权</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </el-header>

          <el-main>
            <router-view></router-view>
          </el-main>

        </el-container>
      </el-container>

      <el-footer style="height: 30px">
        Copyright © 2020 iTennis版权所有
      </el-footer>
    </el-container>
  </div>
</template>

<script>

  import ElSlPanel from "element-ui/packages/color-picker/src/components/sv-panel";

  export default {
    name: 'iTennis',
    data() {
      return {

      }
    },
    components: {ElSlPanel},
    mounted() {
      var asideHeight = document.body.clientHeight;
      document.getElementById("itaside").style.height = (asideHeight - 30) + "px";
    }
  }
</script>

<style scoped>
  .el-header {
    background-color: #CEE;
    color: #333;
    line-height: 60px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
  .el-dropdown{
    height: 60px;
    display: flex;
    align-items: center;
  }

  .el-aside {
    border: 1px solid #EEE;
  }

  .el-footer {
    background-color: #CEE;
    line-height: 30px;
    bottom: 0px;
  }
</style>
