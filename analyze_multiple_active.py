#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
分析生效中记录超过1条的车牌号码
"""

import re
from collections import defaultdict

def parse_duplicate_plates_file(filename):
    """解析duplicate_plates.txt文件"""
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 按车牌号分组
    plate_sections = re.split(r'-{100,}\n车牌号: (.+?) \(共\d+条记录\)\n-{100,}', content)
    
    # 存储每个车牌的生效中记录
    plates_with_multiple_active = {}
    
    for i in range(1, len(plate_sections), 2):
        if i + 1 >= len(plate_sections):
            break
            
        plate_number = plate_sections[i].strip()
        records_text = plate_sections[i + 1]
        
        # 提取所有生效中的记录
        active_records = []
        lines = records_text.strip().split('\n')
        
        for line in lines:
            if '状态: 生效中' in line:
                # 提取完整记录信息
                active_records.append(line.strip())
        
        # 如果生效中的记录超过1条,记录下来
        if len(active_records) > 1:
            plates_with_multiple_active[plate_number] = active_records
    
    return plates_with_multiple_active

def main():
    filename = 'duplicate_plates.txt'
    
    print("正在分析生效中记录超过1条的车牌号码...")
    print("=" * 100)
    
    plates_data = parse_duplicate_plates_file(filename)
    
    # 按生效中记录数量排序
    sorted_plates = sorted(plates_data.items(), key=lambda x: len(x[1]), reverse=True)
    
    print(f"\n找到 {len(sorted_plates)} 个车牌号码有多条生效中记录\n")
    print("=" * 100)
    
    # 输出详细信息
    output_lines = []
    output_lines.append("=" * 100)
    output_lines.append("生效中记录超过1条的车牌号码统计")
    output_lines.append("=" * 100)
    output_lines.append(f"\n总数: {len(sorted_plates)}\n")
    
    for plate_number, active_records in sorted_plates:
        output_lines.append("-" * 100)
        output_lines.append(f"车牌号: {plate_number} (生效中记录: {len(active_records)}条)")
        output_lines.append("-" * 100)
        
        for idx, record in enumerate(active_records, 1):
            output_lines.append(f"  {idx}. {record}")
        
        output_lines.append("")
        
        # 同时打印到控制台
        print(f"\n车牌号: {plate_number} - 生效中记录: {len(active_records)}条")
        for idx, record in enumerate(active_records, 1):
            print(f"  {idx}. {record}")
    
    # 写入文件
    with open('multiple_active_records_detailed.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(output_lines))
    
    print("\n" + "=" * 100)
    print(f"分析完成! 详细结果已保存到: multiple_active_records_detailed.txt")
    print(f"共找到 {len(sorted_plates)} 个车牌号码有多条生效中记录")
    print("=" * 100)
    
    # 生成简要的车牌号列表
    plate_list = [plate for plate, _ in sorted_plates]
    with open('multiple_active_plates_list.txt', 'w', encoding='utf-8') as f:
        f.write(','.join(plate_list))
    
    print(f"\n车牌号列表已保存到: multiple_active_plates_list.txt")

if __name__ == '__main__':
    main()
