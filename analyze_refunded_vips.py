#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VIP记录分析脚本
功能：统计Excel中的VIP记录，按车牌号分组并输出到文件
"""

import pandas as pd
from collections import defaultdict
import sys

def analyze_vip_records(excel_file):
    """
    分析Excel中的VIP记录，按车牌号分组
    
    Args:
        excel_file: Excel文件路径
    """
    print(f"正在读取Excel文件: {excel_file}")
    
    try:
        # 尝试不同的header行
        df = None
        for header_row in [0, 1, 2]:
            try:
                temp_df = pd.read_excel(excel_file, header=header_row)
                print(f"\n尝试header={header_row}:")
                print(f"列名: {temp_df.columns.tolist()}")
                
                # 检查是否找到了车牌号列
                if '车牌号（必填）' in temp_df.columns or any('车牌' in str(col) for col in temp_df.columns):
                    df = temp_df
                    print(f"✓ 找到正确的表头行: {header_row}")
                    break
            except:
                continue
        
        if df is None:
            print("\n错误：无法找到正确的表头行")
            return
        
        print(f"\n总记录数: {len(df)}")
        
        # 查找车牌号列
        plate_col = None
        for col in df.columns:
            if '车牌号' in str(col):
                plate_col = col
                break
        
        if plate_col is None:
            print(f"错误：未找到车牌号列")
            print("可用的列：", df.columns.tolist())
            return
        
        print(f"使用车牌号列: {plate_col}")
        
        # 按车牌号分组
        grouped = defaultdict(list)
        
        # 车牌号正则表达式（中国车牌格式）
        import re
        plate_pattern = re.compile(r'^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]?$')
        
        invalid_plates = []
        
        for idx, row in df.iterrows():
            plate = str(row[plate_col]).strip()
            if plate and plate != 'nan' and plate != '' and plate != 'None':
                # 验证车牌号格式
                if not plate_pattern.match(plate):
                    invalid_plates.append(plate)
                    continue
                
                # 收集该记录的所有信息
                record_info = {}
                for col in df.columns:
                    record_info[col] = row[col]
                grouped[plate].append(record_info)
        
        if invalid_plates:
            print(f"\n发现 {len(invalid_plates)} 个无效车牌号（已过滤）:")
            for plate in set(invalid_plates[:10]):  # 只显示前10个
                print(f"  - {plate}")
            if len(invalid_plates) > 10:
                print(f"  ... 还有 {len(invalid_plates) - 10} 个")
        
        print(f"\n总车牌数: {len(grouped)}")
        
        # 查找VIP状态列
        status_col = None
        for col in df.columns:
            if 'VIP状态' in str(col):
                status_col = col
                break
        
        # 统计已退款的记录
        refunded_plates = {}
        refunded_count = 0
        for plate, records in grouped.items():
            refunded_records = []
            for record in records:
                if status_col and pd.notna(record.get(status_col)):
                    status = str(record.get(status_col)).strip()
                    if status == '已退款':
                        refunded_records.append(record)
                        refunded_count += 1
            if refunded_records:
                refunded_plates[plate] = refunded_records
        
        print(f"已退款记录总数: {refunded_count}")
        print(f"有已退款记录的车牌数: {len(refunded_plates)}")
        
        # 统计有多条记录的车牌
        duplicate_plates = {plate: records for plate, records in grouped.items() if len(records) > 1}
        print(f"有多条记录的车牌数: {len(duplicate_plates)}")
        
        # 输出详细记录到文件
        output_file = 'vip_records_by_plate.txt'
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("=" * 100 + "\n")
            f.write("VIP记录统计（按车牌号分组）\n")
            f.write("=" * 100 + "\n\n")
            
            f.write(f"总记录数: {len(df)}\n")
            f.write(f"总车牌数: {len(grouped)}\n")
            f.write(f"有多条记录的车牌数: {len(duplicate_plates)}\n\n")
            
            # 按车牌号排序
            for plate in sorted(grouped.keys()):
                records = grouped[plate]
                f.write("-" * 100 + "\n")
                f.write(f"车牌号: {plate}\n")
                f.write(f"记录数: {len(records)}\n")
                f.write("-" * 100 + "\n\n")
                
                for i, record in enumerate(records, 1):
                    f.write(f"  记录 {i}:\n")
                    # 显示所有字段
                    for col, value in record.items():
                        if pd.notna(value):  # 只显示非空值
                            f.write(f"    {col}: {value}\n")
                    f.write("\n")
                
                f.write("\n")
        
        print(f"\n详细记录已保存到: {output_file}")
        
        # 输出有多条记录的车牌
        if duplicate_plates:
            duplicate_file = 'duplicate_plates.txt'
            with open(duplicate_file, 'w', encoding='utf-8') as f:
                f.write("=" * 100 + "\n")
                f.write("有多条记录的车牌（需要清理）\n")
                f.write("=" * 100 + "\n\n")
                
                f.write(f"总数: {len(duplicate_plates)}\n\n")
                
                # 按记录数降序排序
                sorted_duplicates = sorted(duplicate_plates.items(), key=lambda x: len(x[1]), reverse=True)
                
                # 找到VIP类型、状态、有效期列
                vip_type_col = None
                status_col = None
                start_time_col = None
                end_time_col = None
                
                for col in df.columns:
                    if 'VIP别称' in str(col) or 'VIP类型' in str(col):
                        vip_type_col = col
                    elif 'VIP状态' in str(col):
                        status_col = col
                    elif '有效期开始' in str(col):
                        start_time_col = col
                    elif '有效期结束' in str(col):
                        end_time_col = col
                
                for plate, records in sorted_duplicates:
                    f.write("-" * 100 + "\n")
                    f.write(f"车牌号: {plate} (共{len(records)}条记录)\n")
                    f.write("-" * 100 + "\n")
                    
                    for i, record in enumerate(records, 1):
                        vip_type = record.get(vip_type_col, 'N/A') if vip_type_col else 'N/A'
                        status = record.get(status_col, 'N/A') if status_col else 'N/A'
                        start_time = record.get(start_time_col, 'N/A') if start_time_col else 'N/A'
                        end_time = record.get(end_time_col, 'N/A') if end_time_col else 'N/A'
                        
                        f.write(f"  {i}. VIP类型: {vip_type}, 状态: {status}, 有效期: {start_time} ~ {end_time}\n")
                    
                    f.write("\n")
                
                # 输出车牌号列表（逗号分隔）
                f.write("\n" + "=" * 100 + "\n")
                f.write("车牌号列表（逗号分隔，可直接用于接口）\n")
                f.write("=" * 100 + "\n\n")
                f.write(','.join(sorted(duplicate_plates.keys())))
                f.write("\n")
            
            print(f"有多条记录的车牌已保存到: {duplicate_file}")
            
            # 打印前10个记录最多的车牌
            print("\n记录数最多的前10个车牌:")
            for i, (plate, records) in enumerate(sorted_duplicates[:10], 1):
                print(f"  {i}. {plate}: {len(records)}条记录")
        else:
            print("\n没有发现重复的车牌记录")
        
        # 输出所有车牌号列表
        all_plates_file = 'all_plates_list.txt'
        with open(all_plates_file, 'w', encoding='utf-8') as f:
            f.write("所有车牌号列表（逗号分隔）\n")
            f.write("=" * 100 + "\n\n")
            f.write(','.join(sorted(grouped.keys())))
            f.write("\n\n")
            f.write("=" * 100 + "\n")
            f.write("所有车牌号列表（每行一个）\n")
            f.write("=" * 100 + "\n\n")
            for plate in sorted(grouped.keys()):
                f.write(f"{plate}\n")
        
        print(f"所有车牌号列表已保存到: {all_plates_file}")
        
        # 统计每个车牌的记录数
        summary_file = 'records_summary.txt'
        with open(summary_file, 'w', encoding='utf-8') as f:
            f.write("记录数统计\n")
            f.write("=" * 100 + "\n\n")
            f.write(f"{'车牌号':<20} {'记录数':>10} {'状态':>15}\n")
            f.write("-" * 100 + "\n")
            
            # 按记录数降序排序
            sorted_plates = sorted(grouped.items(), key=lambda x: len(x[1]), reverse=True)
            for plate, records in sorted_plates:
                status = "需要清理" if len(records) > 1 else "正常"
                f.write(f"{plate:<20} {len(records):>10} {status:>15}\n")
        
        print(f"记录数统计已保存到: {summary_file}")
        
        # 输出已退款记录
        if refunded_plates:
            refunded_file = 'refunded_vips.txt'
            with open(refunded_file, 'w', encoding='utf-8') as f:
                f.write("=" * 100 + "\n")
                f.write("已退款的VIP记录\n")
                f.write("=" * 100 + "\n\n")
                
                f.write(f"已退款记录总数: {refunded_count}\n")
                f.write(f"有已退款记录的车牌数: {len(refunded_plates)}\n\n")
                
                # 找到相关列
                vip_type_col = None
                start_time_col = None
                end_time_col = None
                owner_col = None
                phone_col = None
                
                for col in df.columns:
                    if 'VIP别称' in str(col):
                        vip_type_col = col
                    elif '有效期开始' in str(col):
                        start_time_col = col
                    elif '有效期结束' in str(col):
                        end_time_col = col
                    elif '车主姓名' in str(col):
                        owner_col = col
                    elif '手机号' in str(col):
                        phone_col = col
                
                # 按已退款记录数降序排序
                sorted_refunded = sorted(refunded_plates.items(), key=lambda x: len(x[1]), reverse=True)
                
                for plate, records in sorted_refunded:
                    f.write("-" * 100 + "\n")
                    f.write(f"车牌号: {plate} (共{len(records)}条已退款记录)\n")
                    f.write("-" * 100 + "\n\n")
                    
                    for i, record in enumerate(records, 1):
                        vip_type = record.get(vip_type_col, 'N/A') if vip_type_col else 'N/A'
                        owner = record.get(owner_col, 'N/A') if owner_col else 'N/A'
                        phone = record.get(phone_col, 'N/A') if phone_col else 'N/A'
                        start_time = record.get(start_time_col, 'N/A') if start_time_col else 'N/A'
                        end_time = record.get(end_time_col, 'N/A') if end_time_col else 'N/A'
                        status = record.get(status_col, 'N/A') if status_col else 'N/A'
                        
                        f.write(f"  记录 {i}:\n")
                        f.write(f"    VIP类型: {vip_type}\n")
                        f.write(f"    车主姓名: {owner}\n")
                        f.write(f"    手机号: {phone}\n")
                        f.write(f"    有效期: {start_time} ~ {end_time}\n")
                        f.write(f"    状态: {status}\n")
                        f.write("\n")
                    
                    f.write("\n")
                
                # 输出已退款车牌号列表（逗号分隔）
                f.write("\n" + "=" * 100 + "\n")
                f.write("已退款车牌号列表（逗号分隔）\n")
                f.write("=" * 100 + "\n\n")
                f.write(','.join(sorted(refunded_plates.keys())))
                f.write("\n\n")
                
                # 输出已退款车牌号列表（每行一个）
                f.write("=" * 100 + "\n")
                f.write("已退款车牌号列表（每行一个）\n")
                f.write("=" * 100 + "\n\n")
                for plate in sorted(refunded_plates.keys()):
                    f.write(f"{plate}\n")
            
            print(f"已退款记录已保存到: {refunded_file}")
            
            # 打印前10个已退款记录最多的车牌
            sorted_refunded = sorted(refunded_plates.items(), key=lambda x: len(x[1]), reverse=True)
            print("\n已退款记录最多的前10个车牌:")
            for i, (plate, records) in enumerate(sorted_refunded[:10], 1):
                print(f"  {i}. {plate}: {len(records)}条已退款记录")
        else:
            print("\n没有发现已退款的记录")
        
    except FileNotFoundError:
        print(f"错误：文件 '{excel_file}' 不存在")
    except Exception as e:
        print(f"错误：{str(e)}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("使用方法: python analyze_refunded_vips.py <Excel文件路径>")
        print("示例: python analyze_refunded_vips.py VIP开通、续费导出20260126085653.xls")
        sys.exit(1)
    
    excel_file = sys.argv[1]
    analyze_vip_records(excel_file)
