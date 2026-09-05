package com.maple.quantum_chromodynamic_charge.common;

import com.gto.registrylib.util.entry.AttachmentTypeEntry;
import com.mapleutillib.utils.task.LevelTaskData;
import com.mapleutillib.utils.task.TaskHandler;

import static com.maple.quantum_chromodynamic_charge.QuantumChromodynamicChargeMod.REGISTRY;

public class QCCLevelTask {

    public static final AttachmentTypeEntry<LevelTaskData> LEVEL_TASK_DATA = REGISTRY
            .attachmentType("level_task_data", ignored -> new LevelTaskData())
            .serialize(LevelTaskData.CODEC)
            .register();

    public static final TaskHandler.Tasks TASKS = TaskHandler.tasks(LEVEL_TASK_DATA);
}
