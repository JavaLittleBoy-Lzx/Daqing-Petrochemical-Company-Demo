#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
统计退款记录在有效期内但没有生效中记录的车牌号码
"""

import re
from datetime import datetime
from collections import defaultdict

def parse_duplicate_plates_file(filename):
    """解析duplicate_plates.txt文件"""
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 按车牌号分组
    plate_sections = re.split(r'-{100,}\n车牌号: (.+?) \(共\d+条记录\)\n-{100,}', content)
    
    # 存储每个车牌的记录
    plates_data = {}
    
    for i in range(1, len(plate_sections), 2):
        if i + 1 >= len(plate_sections):
            break
            
        plate_number = plate_sections[i].strip()
        records_text = plate_sections[i + 1]
        
        # 提取所有记录
        refunded_records = []
        active_records = []
        
        lines = records_text.strip().split('\n')
        
        for line in lines:
            if '状态: 已退款' in line:
                refunded_records.append(line.strip())
            elif '状态: 生效中' in line:
                active_records.append(line.strip())
        
        plates_data[plate_number] = {
            'refunded': refunded_records,
            'active': active_records
        }
    
    return plates_data

def parse_date(date_str):
    """解析日期字符串"""
    try:
        return datetime.strptime(date_str, '%Y-%m-%d %H:%M:%S')
    except:
        try:
            return datetime.strptime(date_str, '%Y-%m-%d')
        except:
            return None

def is_valid_now(valid_period_str):
    """判断有效期是否包含当前时间"""
    # 提取有效期
    match = re.search(r'有效期: (.+?) ~ (.+?)(?:\s|$)', valid_period_str)
    if not match:
        return False
    
    start_str = match.group(1).strip()
    end_str = match.group(2).strip()
    
    start_date = parse_date(start_str)
    end_date = parse_date(end_str)
    
    if not start_date or not end_date:
        return False
    
    # 当前时间 2026-01-26
    current_time = datetime(2026, 1, 26)
    
    return start_date <= current_time <= end_date

def main():
    filename = 'duplicate_plates.txt'
    
    print("正在分析退款记录在有效期内但没有生效中记录的车牌号码...")
    print("=" * 100)
    
    plates_data = parse_duplicate_plates_file(filename)
    
    # 筛选符合条件的车牌
    result_plates = {}
    
    for plate_number, records in plates_data.items():
        # 如果有生效中记录,跳过
        if records['active']:
            continue
        
        # 检查是否有在有效期内的退款记录
        valid_refunded = []
        for refund_record in records['refunded']:
            if is_valid_now(refund_record):
                valid_refunded.append(refund_record)
        
        if valid_refunded:
            result_plates[plate_number] = valid_refunded
    
    # 输出结果
    print(f"\n找到 {len(result_plates)} 个车牌号码符合条件\n")
    print("=" * 100)
    
    output_lines = []
    output_lines.append("=" * 100)
    output_lines.append("退款记录在有效期内但没有生效中记录的车牌号码")
    output_lines.append("=" * 100)
    output_lines.append(f"\n总数: {len(result_plates)}\n")
    output_lines.append(f"统计时间: 2026-01-26\n")
    
    # 按车牌号排序
    sorted_plates = sorted(result_plates.items())
    
    for plate_number, refunded_records in sorted_plates:
        output_lines.append("-" * 100)
        output_lines.append(f"车牌号: {plate_number} (有效期内的退款记录: {len(refunded_records)}条)")
        output_lines.append("-" * 100)
        
        for idx, record in enumerate(refunded_records, 1):
            output_lines.append(f"  {idx}. {record}")
        
        output_lines.append("")
        
        # 同时打印到控制台
        print(f"\n车牌号: {plate_number} - 有效期内的退款记录: {len(refunded_records)}条")
        for idx, record in enumerate(refunded_records, 1):
            print(f"  {idx}. {record}")
    
    # 写入文件
    with open('valid_refunded_no_active_records.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(output_lines))
    
    print("\n" + "=" * 100)
    print(f"分析完成! 详细结果已保存到: valid_refunded_no_active_records.txt")
    print(f"共找到 {len(result_plates)} 个车牌号码符合条件")
    print("=" * 100)
    
    # 生成车牌号列表
    plate_list = [plate for plate, _ in sorted_plates]
    with open('valid_refunded_no_active_plates_list.txt', 'w', encoding='utf-8') as f:
        f.write(','.join(plate_list))
    
    print(f"\n车牌号列表已保存到: valid_refunded_no_active_plates_list.txt")
    
    # 生成统计摘要
    print("\n" + "=" * 100)
    print("统计摘要:")
    print("=" * 100)
    print(f"1. 有效期内的退款记录但无生效中记录的车牌数: {len(result_plates)}")
    print(f"2. 这些车牌可能存在数据异常,需要检查")
    print("=" * 100)

if __name__ == '__main__':
    main()
