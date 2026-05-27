package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockGroupOrderVO {

    private LockedGroupOrderVO order;

    private List<LockAllocationVO> allocations;
}
