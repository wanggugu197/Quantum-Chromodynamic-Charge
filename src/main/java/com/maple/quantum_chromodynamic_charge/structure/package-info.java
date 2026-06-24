/**
 * 结构放置系统（GTO platforms 兼容）。
 *
 * <h2>子包职责</h2>
 * <ul>
 * <li>{@link com.maple.quantum_chromodynamic_charge.structure.model} — 领域模型（Structure / Preset）</li>
 * <li>{@link com.maple.quantum_chromodynamic_charge.structure.material} — 材料类型、银行表、装卸逻辑、组件物品</li>
 * <li>{@link com.maple.quantum_chromodynamic_charge.structure.io} — .mbs / mapping JSON / 资源路径</li>
 * <li>{@link com.maple.quantum_chromodynamic_charge.structure.transform} — 旋转镜像与局部坐标</li>
 * <li>{@link com.maple.quantum_chromodynamic_charge.structure.place} — 分 tick 放置</li>
 * <li>{@link com.maple.quantum_chromodynamic_charge.structure.export} — 世界区域导出</li>
 * <li>{@link com.maple.quantum_chromodynamic_charge.structure.registry} — 预设注册与内置数据</li>
 * </ul>
 *
 * <p>
 * 方块实体入口：{@code common.block.StructurePlacerBlockEntity}。
 * </p>
 */
package com.maple.quantum_chromodynamic_charge.structure;
