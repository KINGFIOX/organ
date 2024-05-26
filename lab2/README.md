# REAMDE - lab2

## dependencies

下面是我的环境搭建过程。

1.  `curl -s "https://get.sdkman.io" | bash`
2.  `sdk install sbt`
3.  `sdk install java`
4.  `sdk install scala 2.12.13`
5.  verilator 推荐编译安装

## xmake

尽管这个项目有一个 xmake, 但是 xmake 没啥用，只是服务于 lsp 的，
我是在 vscode 中使用了 clangd 作为 C++ 的 lsp 和 linter。
xmake 可以帮我生成 compile_commands.json 文件，可以用于 clangd 查找头文件

xmake 可以通过 vscode 的 `XMake: UpdateIntellisense` 来生成 json 配置到 .vscode 文件夹中

## chisel template

`https://github.com/chipsalliance/chisel-template/`

## makefile

主要看一下 makefile 的配置

```makefile
# 需要修改的配置
TOP_MODULE = Booth  # 对应于 src/main/scala/xxx/<TOP_MODULE>， 这个 TOP_MODULE 应该遵循 java 的规范，类的名字与文件名相同
MODULE_DIR = booth  # 对应于 src/main/scala/<MODULE_DIR>
WIDTH = 8
```

scala 中有单例对象 object, 应该与 TOP_MODULE 同名，用于 emit verilog code

## script/sed.py

这个文件是用于: scala 生成的 verilog 中有 io*xxx ， 把这个 io* 前缀去掉，
然后把 clock -> clk, reset -> rst

## src/test/cxx

这个文件夹下面，是 c++ 写的 testbench。广泛的用到了来自编译器的宏定义(需要注意)

### inc.h

这里规定了一些函数，原码/补码之间的转换

以及我自定义了一个 int4_t 类型，我觉得我这个类型写的很好。
可以就像是正常的使用一个 int 类型

- 重载了 左值的 operator=
- 重载了 `ostream operator<<`
- 重载了 强制转换 `operator int8_t()`
