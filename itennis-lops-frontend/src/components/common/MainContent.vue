<template>
  <div id="MainContent">
    <el-row>
      <el-col :span="12">
        <div style="padding: 10px">
          <div style="text-align: left;padding-bottom: 5px">
            <b>步骤一:选择功能项</b>
          </div>
          <div class="block">
            <el-cascader
              ref="cascader"
              style="width: 100%;padding-bottom:10px"
              placeholder="试试搜索：设备"
              :options="options"
              @change="change"
              filterable></el-cascader>
          </div>
        </div>
        <div style="padding: 10px">
          <div style="text-align: left;padding-bottom: 5px">
            <b>步骤二:添加配置项</b>
          </div>

          <el-input
            type="textarea"
            :autosize="{ minRows: 30, maxRows: 30}"
            :placeholder=areaconfig
            @change="textareaConfigChange"
            v-model="textareaConfig">
          </el-input>
          <div class="itbtn">
            <el-button type="danger" @click="cleanbtn" plain>配置清除</el-button>
            <el-button type="success" @click="checkbtn" plain>配置格式化</el-button>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div style="padding: 10px">
          <div style="text-align: left;padding-bottom: 5px">
            <b>步骤三:参数配置执行</b>
          </div>
          <div style="text-align: right">

            <div id="basecmd" v-show="basecmd">
              <el-input v-model="inputcmd" style="padding-bottom:10px" id="execcmd" :placeholder=dateinit></el-input>
            </div>
            <div id="highcmd" v-show="highcmd">
              <el-input
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 2}"
                :placeholder=areaparams
                v-model="areaparamsConfig">
              </el-input>
            </div>

            <div id="basescp" v-show="basescp">
              <el-input v-model="inputscp1" style="" placeholder="本地输入源"></el-input>
              <span style="font-size: 14px">至</span>
              <el-input v-model="inputscp2" style="padding-bottom:10px" placeholder="远程输出源"></el-input>
            </div>

            <div id="toolsqx" v-show="toolsqx">
              <el-input v-model="inputqx1" style="padding-bottom:10px" placeholder="查找指定路径内容"></el-input>
              <el-input v-model="inputqx2" style="padding-bottom:10px"
                        placeholder="填写查找权限(例如:750或者!750，可为空)"></el-input>
              <el-input v-model="inputqx3" style="padding-bottom:10px"
                        placeholder="填写查找用户(例如:root或者!root，可为空)"></el-input>
            </div>


            <div class="block">
              <el-button type="primary" :disabled="isExec" @click="execbtn" plain>命令执行</el-button>
            </div>

          </div>
          <div>
            <el-input
              style="padding-top: 10px"
              type="textarea"
              :disabled="true"
              :autosize="{ minRows: 30, maxRows: 30}"
              placeholder=">>>运行结果输出"
              v-model="textareaResult">
            </el-input>
          </div>

        </div>

      </el-col>
    </el-row>
    <el-table ref="multipleTable" :data="showList" :header-cell-style="{background:'#96CDCD'}"
              stripe tooltip-effect="dark" style="width: 100%;margin-top:1%;">
      <el-table-column :label="item.propName" :property="item.prop" v-for="item in tableColumnList" :key="item.prop"
                       align="center">
        <template slot-scope="scope">
          <span>{{scope.row[scope.column.property]}}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>

  import axios from 'axios';

  export default {
    name: "MainContent",
    components: {},
    data() {
      return {
        basecmd: false,
        basescp: false,
        toolsqx: false,
        highcmd: false,
        isExec: true,
        areaconfig: "#[临时IP地址  | 临时IP地址]\t用户名\t密码",
        dateinit: '输入需要执行的命令...',
        areaparams: '',
        textareaConfig: '',
        textareaResult: '',
        areaparamsConfig: '',
        inputcmd: '',
        inputscp1: '',
        inputscp2: '',
        inputqx1: '',
        inputqx2: '',
        inputqx3: '',
        tableColumnList: [],
        showList: [],
        resultData: [],
        pickerOptions: {
          shortcuts: [{
            text: '今天',
            onClick(picker) {
              picker.$emit('pick', new Date());
            }
          }]
        },
        value2: '',
        options: [
          {
            value: 'baseFunsion',
            label: '基础功能',
            children: [{
              value: 'baseplml',
              label: '批量命令执行',
            }, {
              value: 'baseplsc',
              label: '批量文件上传',
            }, {
              value: 'baseplxz',
              label: '批量文件下载',
            }]
          }, {
            value: 'highFunsion',
            label: '高级功能',
            children: [{
              value: 'highpxe',
              label: 'PXE服务配置',
            }, {
              value: 'highcip',
              label: '批量更换ip地址',
            }, {
              value: 'highpzwkbd',
              label: '批量配置网卡绑定',
            }, {
              value: 'highpzbmc',
              label: '批量配置BMC地址',
            }, {
              value: 'highpzraid',
              label: '批量配置RAID',
            }]
          }, {
            value: 'toolsFunsion',
            label: '常用工具',
            children: [{
              value: 'toolslld',
              label: '设备信息收集',
            }, {
              value: 'toolstzpz',
              label: '时间时区配置',
            }, {
              value: 'toolsrypz',
              label: '批量远程YUM配置',
            }, {
              value: 'toolscmbr',
              label: '清除MBR引导',
            }, {
              value: 'toolscpwd',
              label: '修改用户密码',
            }, {
              value: 'toolsmm',
              label: '服务器免密配置',
            }, {
              value: 'toolscqx',
              label: '检查文件权限属组',
            }]
          }]
      }
    },
    methods: {
      textareaConfigChange(val) {
        this.isExec = true;
      },
      change(val) {
        //控制标签显示隐藏
        let nodesObj = this.$refs['cascader'].getCheckedNodes();
        let nodeobj = nodesObj[0].data;
        if (nodeobj.value == "baseplml") {
          this.dateinit = "输入需要执行的命令...";
          this.inputcmd = '';
          this.basecmd = true
        } else if (nodeobj.value == "toolstzpz") {
          var d = new Date();
          this.dateinit = "请输入需要同步的时间";
          this.inputcmd = d.getFullYear() + "-" + d.getMonth() + "-" + d.getDay() + " " + d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds();
          this.basecmd = true
        } else if (nodeobj.value == "toolsrypz") {
          this.inputcmd = '';
          this.basecmd = true;
          this.dateinit = "请输入镜像需要挂载本地的路径或者远程地址";
        } else {
          this.basecmd = false
        }
        if (nodeobj.value == "highpxe") {
          this.inputcmd = '';
          this.highcmd = true;
          this.areaparams = "根据格式配置PXE服务器参数";
          this.areaparamsConfig = "# pxe服务器的信息\n" +
            "itennis:\n" +
            "  pxeServer:\n" +
            "    ip: 192.168.0.199\n" +
            "    netmask: 255.255.255.0\n" +
            "    user: root\n" +
            "    password: 123456\n" +
            "    interface: ens33\n" +
            "    os: redhat\n" +
            "    version: 7.3\n" +
            "    iso: /opt/rhel-server-7.3-x86_64-dvd.iso\n" +
            "\n" +
            "  # pxe安装的操作系统信息\n" +
            "  installOS:\n" +
            "    os: redhat\n" +
            "    version: 7.3\n" +
            "    password: 123456\n" +
            "    interface: ens33\n" +
            "    iso: /opt/rhel-server-7.3-x86_64-dvd.iso\n" +
            "    driver:\n" +
            "\n" +
            "  # pxe安装的操作系统的分区\n" +
            "  partitions:\n" +
            "    - {path: /,fstype: ext4,size: 20480,ondisk: sda}\n" +
            "    - {path: /tmp,fstype: ext4,size: 10240,ondisk: sda}\n" +
            "    - {path: /var,fstype: ext4,size: 10240,ondisk: sda}\n" +
            "    - {path: /var/log,fstype: ext4,size: 153600,ondisk: sda}\n" +
            "    - {path: /srv/BigData,fstype: ext4,size: 61440,ondisk: sda}\n" +
            "    - {path: /opt,fstype: ext4,size: 1,ondisk: sda}\n" +
            "\n" +
            "  # DHCP服务器的信息\n" +
            "  dhcpServer:\n" +
            "    ip: 192.168.0.199\n" +
            "    netmask: 255.255.255.0\n" +
            "    subnet: 192.168.0.0\n" +
            "    start: 192.168.0.100\n" +
            "    end: 192.168.0.200\n" +
            "\n" +
            "  # 系统需要安装的软件包\n" +
            "  packages:\n" +
            "    - tftp-server\n" +
            "    - dhcp\n" +
            "    - xinetd\n" +
            "    - httpd\n" +
            "    - redhat-lsb\n" +
            "    - expect\n" +
            "    - syslinux\n" +
            "    - tree\n" +
            "    - vsftpd\n" +
            "    - ipmitool\n" +
            "    - expect";
        } else {
          this.highcmd = false;
        }
        if (nodeobj.value == "toolscqx") {
          this.toolsqx = true;
        } else {
          this.toolsqx = false;
        }

        if (nodeobj.value == "baseplsc" || nodeobj.value == "baseplxz") {
          this.basescp = true
        } else {
          this.basescp = false
        }

        //设置textarea显示
        if (nodeobj.value == "highpzbmc") {
          this.areaconfig = "#临时IP(提供SSH)\t用户名\t密码\tBMC地址\tBMC掩码\tBMC网关"
        } else if (nodeobj.value == "highpzwkbd") {
          this.areaconfig = "#临时IP地址\t用户名\t密码\t[绑定接口\t模式\t绑定地址\t掩码\t网关 子接口1\t子接口2]\t...\t[绑定接口\t模式\t绑定地址\t掩码\t网关 子接口1\t子接口2]"
        } else if (nodeobj.value == "highcip") {
          this.areaconfig = "#用户名\t密码\t[旧IP\t新IP\t新掩码\t新网关]\t...\t[旧IP\t新IP\t新掩码\t新网关]"
        } else if (nodeobj.value == "toolscpwd") {
          this.areaconfig = "#临时IP地址\t用户名\t旧密码  新密码"
        } else if (nodeobj.value == "highpzraid") {
          this.areaconfig = "#IP地址(提供SSH连接)\t用户名\t密码\tRAID模式\t[模式:盘数:几组,模式:盘数:几组]"
        } else {
          this.areaconfig = "#[临时IP地址  | 临时IP地址]\t用户名\t密码"
        }

        this.textareaResult = '';
      },
      execbtn() {
        this.textareaResult = '';
        let nodesObj = this.$refs['cascader'].getCheckedNodes();
        if (nodesObj.length != 0) {
          // 配置命令执行
          if (nodesObj[0].data.value == "baseplml") {
            axios.post("http://127.0.0.1:8088/exec/config", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf",
              cmds: this.inputcmd
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);

            });
          }
          // 配置时间时区
          if (nodesObj[0].data.value == "toolstzpz") {
            axios.post("http://127.0.0.1:8088/exec/timezone", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf",
              cmds: this.inputcmd
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置文件上传
          if (nodesObj[0].data.value == "baseplsc") {
            axios.post("http://127.0.0.1:8088/updown/config", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf",
              remote: this.inputscp2,
              local: this.inputscp1,
              mode: "upload",
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置文件下载
          if (nodesObj[0].data.value == "baseplxz") {
            axios.post("http://127.0.0.1:8088/updown/config", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf",
              remote: this.inputscp2,
              local: this.inputscp1,
              mode: "download",
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置PXE
          if (nodesObj[0].data.value == "highpxe") {
            axios.post("http://127.0.0.1:8088/pxe/remote", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf",
              highparams: this.areaparamsConfig
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 批量修改ip地址
          if (nodesObj[0].data.value == "highcip") {
            axios.post("http://127.0.0.1:8088/exec/change/ip", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置双网卡绑定
          if (nodesObj[0].data.value == "highpzwkbd") {
            axios.post("http://127.0.0.1:8088/net/config", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置BMC地址
          if (nodesObj[0].data.value == "highpzbmc") {
            axios.post("http://127.0.0.1:8088/ipmi/config", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置raid
          if (nodesObj[0].data.value == "highpzraid") {
            axios.post("http://127.0.0.1:8088/raid/config", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置检查lld信息
          if (nodesObj[0].data.value == "toolslld") {
            axios.post("http://127.0.0.1:8088/exec/checklld", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置远程Yum
          if (nodesObj[0].data.value == "toolsrypz") {
            axios.post("http://127.0.0.1:8088/exec/config/yum", {
              data: JSON.stringify(this.resultData),
              link: this.inputcmd,
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 清除MBR
          if (nodesObj[0].data.value == "toolscmbr") {
            axios.post("http://127.0.0.1:8088/exec/mbr/clean/all", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 修改密码
          if (nodesObj[0].data.value == "toolscpwd") {
            axios.post("http://127.0.0.1:8088/exec/change/pwd", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 配置集群免密
          if (nodesObj[0].data.value == "toolsmm") {
            axios.post("http://127.0.0.1:8088/exec/config/nopwd/all", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf"
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }
          // 检查权限用户
          if (nodesObj[0].data.value == "toolscqx") {
            axios.post("http://127.0.0.1:8088/exec/find", {
              data: JSON.stringify(this.resultData),
              conf: "a.conf",
              dir: this.inputqx1,
              user: this.inputqx3,
              perm: this.inputqx2,
            }).then((response) => {
              let res = response.data;
              this.textareaResult = JSON.stringify(res["data"]);
            });
          }

        } else {
          alert("请选择功能");
        }

      },
      cleanbtn() {
        this.textareaConfig = '';
        this.textareaResult = '';
        this.tableColumnList = [];
        this.showList = [];
        this.isExec = true;
      },
      checkbtn() {
        this.tableColumnList = [];
        this.showList = [];
        this.resultData = [];
        let list = [];
        let configstr = this.textareaConfig;
        if (configstr.length == 0) {
          alert("请填写配置项");
          return;
        }

        let listconfig = configstr.split(/[\n,]/g);
        for (let i = 0; i < listconfig.length; i++) {
          let line = listconfig[i];
          if (!line.startsWith("#") && !line.startsWith("//")) {
            let col = line.trim().split(/[\s,\t]+/);
            this.resultData.push(col);
            let obj = {};
            for (let j = 0; j < col.length; j++) {
              obj[j] = col[j];
            }
            list.push(obj);
          }
        }
        let keys = Object.keys(list[0]);
        for (let i = 0; i < keys.length; i++) {
          var t = {};
          t["propName"] = "col" + keys[i];
          t["prop"] = keys[i];
          this.tableColumnList.push(t);
        }
        this.showList = list;
        this.isExec = false;
      }
    }
  }
</script>

<style scoped>
  .itbtn {
    padding: 10px;
    text-align: right;
  }

</style>
