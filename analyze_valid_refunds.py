#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
分析已退款记录的有效性，并区分有无生效中记录的车牌
"""

import pandas as pd
from datetime import datetime
from collections import defaultdict
import re

def parse_vip_records(excel_file):
    """解析Excel文件获取所有记录"""
    print(f"正在读取Excel文件: {excel_file}")
    
    # 读取Excel（表头在第2行）
    df = pd.read_excel(excel_file, header=1)
    
    # 查找相关列
    plate_col = None
    vip_type_col = None
    status_col = None
    start_time_col = None
    end_time_col = None
    owner_col = None
    phone_col = None
    
    for col in df.columns:
        if '车牌号' in str(col):
            plate_col = col
        elif 'VIP别称' in str(col):
            vip_type_col = col
        elif 'VIP状态' in str(col):
            status_col = col
        elif '有效期开始' in str(col):
            start_time_col = col
        elif '有效期结束' in str(col):
            end_time_col = col
        elif '车主姓名' in str(col):
            owner_col = col
        elif '手机号' in str(col):
            phone_col = col
    
    return df, plate_col, vip_type_col, status_col, start_time_col, end_time_col, owner_col, phone_col

def is_valid_date(date_str, current_time):
    """判断日期是否在有效期内"""
    if pd.isna(date_str):
        return False
    
    try:
        # 尝试解析日期
        date_str = str(date_str).strip()
        
        # 处理各种日期格式
        for fmt in ['%Y-%m-%d %H:%M:%S', '%Y-%m-%d', '%Y/%m/%d %H:%M:%S', '%Y/%m/%d']:
            try:
                date_obj = datetime.strptime(date_str, fmt)
                return date_obj >= current_time
            except:
                continue
        
        # 如果是pandas的Timestamp对象
        if hasattr(date_str, 'to_pydatetime'):
            return date_str.to_pydatetime() >= current_time
            
        return False
    except:
        return False

def main():
    excel_file = 'VIP开通、续费导出20260126085653.xls'
    current_time = datetime.now()
    
    print(f"当前时间: {current_time.strftime('%Y-%m-%d %H:%M:%S')}\n")
    
    # 解析Excel
    df, plate_col, vip_type_col, status_col, start_time_col, end_time_col, owner_col, phone_col = parse_vip_records(excel_file)
    
    # 车牌号正则表达式
    plate_pattern = re.compile(r'^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]?$')
    
    # 按车牌号分组所有记录
    all_records = defaultdict(list)
    
    for idx, row in df.iterrows():
        plate = str(row[plate_col]).strip()
        if plate and plate != 'nan' and plate_pattern.match(plate):
            record = {
                'plate': plate,
                'vip_type': row.get(vip_type_col, 'N/A'),
                'status': str(row.get(status_col, 'N/A')).strip(),
                'start_time': row.get(start_time_col, 'N/A'),
                'end_time': row.get(end_time_col, 'N/A'),
                'owner': row.get(owner_col, 'N/A'),
                'phone': row.get(phone_col, 'N/A')
            }
            all_records[plate].append(record)
    
    # 分析每个车牌的状态
    plates_with_active = {}  # 有生效中记录的车牌
    plates_without_active = {}  # 没有生效中记录的车牌
    valid_refunded_records = []  # 有效期内的已退款记录
    multiple_active_plates = {}  # 生效中记录超过1条的车牌
    
    for plate, records in all_records.items():
        has_active = False
        refunded_records = []
        active_records = []
        expired_records = []
        
        for record in records:
            status = record['status']
            
            if status == '生效中':
                has_active = True
                active_records.append(record)
            elif status == '已退款':
                refunded_records.append(record)
                # 检查是否在有效期内
                if is_valid_date(record['end_time'], current_time):
                    valid_refunded_records.append(record)
            elif status == '已过期':
                expired_records.append(record)
        
        # 分类存储
        if has_active:
            plates_with_active[plate] = {
                'active': active_records,
                'refunded': refunded_records,
                'expired': expired_records
            }
            # 如果生效中记录超过1条
            if len(active_records) > 1:
                multiple_active_plates[plate] = active_records
        else:
            plates_without_active[plate] = {
                'refunded': refunded_records,
                'expired': expired_records
            }
    
    # 输出统计信息
    print(f"总车牌数: {len(all_records)}")
    print(f"有生效中记录的车牌数: {len(plates_with_active)}")
    print(f"没有生效中记录的车牌数: {len(plates_without_active)}")
    print(f"生效中记录超过1条的车牌数: {len(multiple_active_plates)}")
    print(f"有效期内的已退款记录数: {len(valid_refunded_records)}\n")
    
    # 输出详细报告
    output_file = 'vip_status_analysis.txt'
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("=" * 120 + "\n")
        f.write("VIP记录状态分析报告\n")
        f.write("=" * 120 + "\n\n")
        f.write(f"分析时间: {current_time.strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"总车牌数: {len(all_records)}\n")
        f.write(f"有生效中记录的车牌数: {len(plates_with_active)}\n")
        f.write(f"没有生效中记录的车牌数: {len(plates_without_active)}\n")
        f.write(f"有效期内的已退款记录数: {len(valid_refunded_records)}\n\n")
        
        # 第一部分：有生效中记录的车牌
        f.write("\n" + "=" * 120 + "\n")
        f.write("第一部分：有生效中记录的车牌（共 {} 个）\n".format(len(plates_with_active)))
        f.write("=" * 120 + "\n\n")
        
        for plate in sorted(plates_with_active.keys()):
            data = plates_with_active[plate]
            f.write("-" * 120 + "\n")
            f.write(f"车牌号: {plate}\n")
            f.write(f"  生效中记录: {len(data['active'])} 条\n")
            f.write(f"  已退款记录: {len(data['refunded'])} 条\n")
            f.write(f"  已过期记录: {len(data['expired'])} 条\n")
            f.write("-" * 120 + "\n\n")
            
            # 生效中记录
            if data['active']:
                f.write("  【生效中记录】\n")
                for i, record in enumerate(data['active'], 1):
                    f.write(f"    {i}. VIP类型: {record['vip_type']}\n")
                    f.write(f"       车主姓名: {record['owner']}\n")
                    f.write(f"       手机号: {record['phone']}\n")
                    f.write(f"       有效期: {record['start_time']} ~ {record['end_time']}\n")
                    f.write(f"       状态: {record['status']}\n\n")
            
            # 已退款记录
            if data['refunded']:
                f.write("  【已退款记录】\n")
                for i, record in enumerate(data['refunded'], 1):
                    is_valid = is_valid_date(record['end_time'], current_time)
                    validity_mark = " ✓ 有效期内" if is_valid else " ✗ 已过有效期"
                    f.write(f"    {i}. VIP类型: {record['vip_type']}{validity_mark}\n")
                    f.write(f"       车主姓名: {record['owner']}\n")
                    f.write(f"       手机号: {record['phone']}\n")
                    f.write(f"       有效期: {record['start_time']} ~ {record['end_time']}\n")
                    f.write(f"       状态: {record['status']}\n\n")
            
            # 已过期记录
            if data['expired']:
                f.write(f"  【已过期记录】（共 {len(data['expired'])} 条，已省略详情）\n\n")
            
            f.write("\n")
        
        # 第二部分：没有生效中记录的车牌
        f.write("\n" + "=" * 120 + "\n")
        f.write("第二部分：没有生效中记录的车牌（共 {} 个）\n".format(len(plates_without_active)))
        f.write("=" * 120 + "\n\n")
        
        for plate in sorted(plates_without_active.keys()):
            data = plates_without_active[plate]
            f.write("-" * 120 + "\n")
            f.write(f"车牌号: {plate}\n")
            f.write(f"  已退款记录: {len(data['refunded'])} 条\n")
            f.write(f"  已过期记录: {len(data['expired'])} 条\n")
            f.write("-" * 120 + "\n\n")
            
            # 已退款记录
            if data['refunded']:
                f.write("  【已退款记录】\n")
                for i, record in enumerate(data['refunded'], 1):
                    is_valid = is_valid_date(record['end_time'], current_time)
                    validity_mark = " ✓ 有效期内" if is_valid else " ✗ 已过有效期"
                    f.write(f"    {i}. VIP类型: {record['vip_type']}{validity_mark}\n")
                    f.write(f"       车主姓名: {record['owner']}\n")
                    f.write(f"       手机号: {record['phone']}\n")
                    f.write(f"       有效期: {record['start_time']} ~ {record['end_time']}\n")
                    f.write(f"       状态: {record['status']}\n\n")
            
            # 已过期记录
            if data['expired']:
                f.write(f"  【已过期记录】（共 {len(data['expired'])} 条，已省略详情）\n\n")
            
            f.write("\n")
        
        # 第三部分：车牌号列表
        f.write("\n" + "=" * 120 + "\n")
        f.write("第三部分：车牌号列表\n")
        f.write("=" * 120 + "\n\n")
        
        f.write("有生效中记录的车牌号列表（逗号分隔）：\n")
        f.write("-" * 120 + "\n")
        f.write(','.join(sorted(plates_with_active.keys())))
        f.write("\n\n")
        
        f.write("没有生效中记录的车牌号列表（逗号分隔）：\n")
        f.write("-" * 120 + "\n")
        f.write(','.join(sorted(plates_without_active.keys())))
        f.write("\n")
    
    print(f"详细分析报告已保存到: {output_file}")
    
    # 输出有效期内的已退款记录到单独文件（按车牌号分组）
    if valid_refunded_records:
        # 按车牌号分组
        valid_refunds_by_plate = defaultdict(list)
        for record in valid_refunded_records:
            valid_refunds_by_plate[record['plate']].append(record)
        
        valid_refunds_file = 'valid_refunded_records.txt'
        with open(valid_refunds_file, 'w', encoding='utf-8') as f:
            f.write("=" * 120 + "\n")
            f.write("有效期内的已退款记录（按车牌号分组）\n")
            f.write("=" * 120 + "\n\n")
            f.write(f"总记录数: {len(valid_refunded_records)}\n")
            f.write(f"涉及车牌数: {len(valid_refunds_by_plate)}\n\n")
            
            # 按车牌号排序，按记录数降序
            sorted_plates = sorted(valid_refunds_by_plate.items(), key=lambda x: len(x[1]), reverse=True)
            
            for plate, records in sorted_plates:
                f.write("-" * 120 + "\n")
                f.write(f"车牌号: {plate} (共{len(records)}条有效期内的已退款记录)\n")
                f.write("-" * 120 + "\n\n")
                
                for i, record in enumerate(records, 1):
                    f.write(f"  记录 {i}:\n")
                    f.write(f"    VIP类型: {record['vip_type']}\n")
                    f.write(f"    车主姓名: {record['owner']}\n")
                    f.write(f"    手机号: {record['phone']}\n")
                    f.write(f"    有效期: {record['start_time']} ~ {record['end_time']}\n")
                    f.write(f"    状态: {record['status']}\n\n")
                
                f.write("\n")
            
            # 输出车牌号列表
            f.write("\n" + "=" * 120 + "\n")
            f.write("有效期内已退款记录的车牌号列表\n")
            f.write("=" * 120 + "\n\n")
            
            f.write("车牌号列表（逗号分隔，可直接用于接口）：\n")
            f.write("-" * 120 + "\n")
            f.write(','.join(sorted(valid_refunds_by_plate.keys())))
            f.write("\n\n")
            
            f.write("车牌号列表（每行一个）：\n")
            f.write("-" * 120 + "\n")
            for plate in sorted(valid_refunds_by_plate.keys()):
                f.write(f"{plate}\n")
            
            f.write("\n")
            f.write("=" * 120 + "\n")
            f.write("记录数统计（按车牌号）\n")
            f.write("=" * 120 + "\n\n")
            
            for plate, records in sorted_plates:
                f.write(f"{plate}: {len(records)}条有效期内的已退款记录\n")
        
        print(f"有效期内的已退款记录已保存到: {valid_refunds_file}")
        print(f"涉及车牌数: {len(valid_refunds_by_plate)}")
    
    # 输出生效中记录超过1条的车牌
    if multiple_active_plates:
        multiple_active_file = 'multiple_active_records.txt'
        with open(multiple_active_file, 'w', encoding='utf-8') as f:
            f.write("=" * 120 + "\n")
            f.write("生效中记录超过1条的车牌（需要清理）\n")
            f.write("=" * 120 + "\n\n")
            f.write(f"涉及车牌数: {len(multiple_active_plates)}\n\n")
            
            # 按生效中记录数降序排序
            sorted_multiple = sorted(multiple_active_plates.items(), key=lambda x: len(x[1]), reverse=True)
            
            for plate, active_records in sorted_multiple:
                f.write("-" * 120 + "\n")
                f.write(f"车牌号: {plate} (共{len(active_records)}条生效中记录)\n")
                f.write("-" * 120 + "\n\n")
                
                for i, record in enumerate(active_records, 1):
                    f.write(f"  记录 {i}:\n")
                    f.write(f"    VIP类型: {record['vip_type']}\n")
                    f.write(f"    车主姓名: {record['owner']}\n")
                    f.write(f"    手机号: {record['phone']}\n")
                    f.write(f"    有效期: {record['start_time']} ~ {record['end_time']}\n")
                    f.write(f"    状态: {record['status']}\n\n")
                
                f.write("\n")
            
            # 输出车牌号列表
            f.write("\n" + "=" * 120 + "\n")
            f.write("生效中记录超过1条的车牌号列表\n")
            f.write("=" * 120 + "\n\n")
            
            f.write("车牌号列表（逗号分隔，可直接用于接口）：\n")
            f.write("-" * 120 + "\n")
            f.write(','.join(sorted(multiple_active_plates.keys())))
            f.write("\n\n")
            
            f.write("车牌号列表（每行一个）：\n")
            f.write("-" * 120 + "\n")
            for plate in sorted(multiple_active_plates.keys()):
                f.write(f"{plate}\n")
            
            f.write("\n")
            f.write("=" * 120 + "\n")
            f.write("记录数统计（按车牌号）\n")
            f.write("=" * 120 + "\n\n")
            
            for plate, active_records in sorted_multiple:
                f.write(f"{plate}: {len(active_records)}条生效中记录\n")
        
        print(f"生效中记录超过1条的车牌已保存到: {multiple_active_file}")
        print(f"涉及车牌数: {len(multiple_active_plates)}")

if __name__ == '__main__':
    main()
