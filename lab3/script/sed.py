import re

def process_file(input_file, output_file):
    # 打开输入文件进行读取
    with open(input_file, 'r') as file:
        text = file.read()

    # 执行替换操作
    # 删除所有标识符的io_前缀
    text = re.sub(r'\bio_', '', text)
    # 将所有的clock改成clk
    text = text.replace('clock', 'cpu_clk')
    # 将所有reset改成rst
    text = text.replace('reset', 'cpu_rst')

    # 写入到输出文件
    with open(output_file, 'w') as file:
        file.write(text)

    print(f"Processed file saved as '{output_file}'")

# 脚本使用示例
input_filename = 'DCache.sv'  # 指定输入文件的路径
output_filename = '/home/wangfiox/Desktop/miniRV_axi/miniRV_axi.srcs/sources_1/new/DCache.v'  # 指定输出文件的路径
process_file(input_filename, output_filename)
