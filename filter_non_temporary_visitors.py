#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从有效期内退款但无生效中记录的车牌中,筛选出VIP类型不是"临时来访(化工西门)"的车牌
"""

import re

def main():
    # 目标车牌列表
    target_plates = [
        '黑CFB9958', '黑E2080U', '黑E3CA62', '黑E6056C', '黑E627KW', '黑E6962N',
        '黑E7105L', '黑E82G10', '黑E8891S', '黑E97GP8', '黑ED12527', '黑EF50377',
        '黑EFF1939', '黑EH1657', '黑EJ0776', '黑EJ3363', '黑EJ7192', '黑EK0804',
        '黑EL1049', '黑EL3630', '黑EL3708', '黑EL6122', '黑EL8379', '黑EL8756',
        '黑EP2999', '黑EP6535', '黑ER4688', '黑ERB000', '黑J27171', '黑M02238',
        '黑M2D002', '黑MB2064', '黑MG9853', '黑MG9945', '黑MM8362'
    ]
    
    # 读取详细记录文件
    with open('valid_refunded_no_active_records.txt', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 按车牌号分组解析
    plate_sections = re.split(r'-{100,}\n车牌号: (.+?) \(有效期内的退款记录: \d+条\)\n-{100,}', content)
    
    # 存储非临时来访的车牌及其记录
    non_temporary_plates = {}
    
    for i in range(1, len(plate_sections), 2):
        if i + 1 >= len(plate_sections):
            break
        
        plate_number = plate_sections[i].strip()
        
        # 只处理目标车牌
        if plate_number not in target_plates:
            continue
        
        records_text = plate_sections[i + 1]
        lines = records_text.strip().split('\n')
        
        # 筛选非临时来访的记录
        non_temporary_records = []
        for line in lines:
            if line.strip() and '临时来访（化工西门）' not in line:
                non_temporary_records.append(line.strip())
        
        # 如果有非临时来访的记录,保存
        if non_temporary_records:
            non_temporary_plates[plate_number] = non_temporary_records
    
    # 输出结果
    print("=" * 100)
    print("VIP类型不是'临时来访(化工西门)'的车牌号码")
    print("=" * 100)
    print(f"\n总数: {len(non_temporary_plates)}\n")
    
    output_lines = []
    output_lines.append("=" * 100)
    output_lines.append("VIP类型不是'临时来访(化工西门)'的车牌号码")
    output_lines.append("=" * 100)
    output_lines.append(f"\n总数: {len(non_temporary_plates)}\n")
    output_lines.append(f"统计时间: 2026-01-26\n")
    
    # 按车牌号排序
    sorted_plates = sorted(non_temporary_plates.items())
    
    for plate_number, records in sorted_plates:
        output_lines.append("-" * 100)
        output_lines.append(f"车牌号: {plate_number} (非临时来访记录: {len(records)}条)")
        output_lines.append("-" * 100)
        
        for record in records:
            output_lines.append(f"  {record}")
        
        output_lines.append("")
        
        # 同时打印到控制台
        print(f"\n车牌号: {plate_number} - 非临时来访记录: {len(records)}条")
        for record in records:
            print(f"  {record}")
    
    # 写入文件
    with open('non_temporary_visitor_records.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(output_lines))
    
    print("\n" + "=" * 100)
    print(f"分析完成! 详细结果已保存到: non_temporary_visitor_records.txt")
    print(f"共找到 {len(non_temporary_plates)} 个车牌号码符合条件")
    print("=" * 100)
    
    # 生成车牌号列表
    plate_list = [plate for plate, _ in sorted_plates]
    with open('non_temporary_visitor_plates_list.txt', 'w', encoding='utf-8') as f:
        f.write(','.join(plate_list))
    
    print(f"\n车牌号列表已保存到: non_temporary_visitor_plates_list.txt")
    print(f"\n筛选出的车牌号: {','.join(plate_list)}")

if __name__ == '__main__':
    main()
