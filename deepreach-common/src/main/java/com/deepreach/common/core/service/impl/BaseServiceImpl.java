package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.mapper.BaseMapper;
import com.deepreach.common.core.service.BaseService;
import com.deepreach.common.core.domain.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 通用Service实现类
 *
 * @param <M> Mapper类型
 * @param <T> 实体类型
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T extends BaseEntity> implements BaseService<T> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected M baseMapper;

    /**
     * 获取主键值
     * 默认实现，子类可以重写
     *
     * @param entity 实体对象
     * @return 主键值
     */
    protected Serializable getId(T entity) {
        // 默认实现，通过反射尝试获取ID字段
        // 子类可以重写此方法提供具体的ID获取逻辑
        try {
            // 尝试常见的ID字段名
            String[] possibleIdFields = {"id", "Id", "ID", "userId", "priceId", "deptId", "roleId"};

            for (String fieldName : possibleIdFields) {
                try {
                    java.lang.reflect.Field field = entity.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value != null) {
                        return (Serializable) value;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // 继续尝试下一个字段名
                    continue;
                }
            }

            // 如果没找到ID字段，返回null
            return null;
        } catch (Exception e) {
            log.warn("获取实体ID时发生异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public T selectById(Serializable id) {
        if (id == null) {
            return null;
        }
        return baseMapper.selectById(id);
    }

    @Override
    public List<T> selectAll() {
        return baseMapper.selectAll();
    }

    @Override
    public List<T> selectByCondition(T entity) {
        if (entity == null) {
            return baseMapper.selectAll();
        }
        return baseMapper.selectByCondition(entity);
    }

    @Override
    public int insert(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为空");
        }

        // 验证实体
        String validation = validate(entity);
        if (validation != null) {
            throw new IllegalArgumentException(validation);
        }

        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为空");
        }

        Serializable id = getId(entity);
        if (id == null) {
            throw new IllegalArgumentException("主键不能为空");
        }

        // 验证实体
        String validation = validate(entity);
        if (validation != null) {
            throw new IllegalArgumentException(validation);
        }

        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Serializable id) {
        if (id == null) {
            throw new IllegalArgumentException("主键不能为空");
        }
        return baseMapper.deleteById(id);
    }

    @Override
    public int deleteByIds(Serializable[] ids) {
        if (ids == null || ids.length == 0) {
            throw new IllegalArgumentException("主键数组不能为空");
        }
        return baseMapper.deleteByIds(ids);
    }

    @Override
    public int deleteByCondition(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("删除条件不能为空");
        }
        return baseMapper.deleteByCondition(entity);
    }

    @Override
    public int selectCount(T entity) {
        return baseMapper.selectCount(entity);
    }

    @Override
    public List<T> selectPage(T entity, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        return baseMapper.selectPage(entity, pageNum, pageSize);
    }

    @Override
    public int batchInsert(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("实体列表不能为空");
        }

        // 验证所有实体
        for (T entity : entities) {
            String validation = validate(entity);
            if (validation != null) {
                throw new IllegalArgumentException("批量插入验证失败: " + validation);
            }
        }

        return baseMapper.batchInsert(entities);
    }

    @Override
    public int batchUpdate(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("实体列表不能为空");
        }

        // 验证所有实体
        for (T entity : entities) {
            String validation = validate(entity);
            if (validation != null) {
                throw new IllegalArgumentException("批量更新验证失败: " + validation);
            }
        }

        return baseMapper.batchUpdate(entities);
    }

    @Override
    public String validate(T entity) {
        // 默认实现，子类可以重写
        return null;
    }

    @Override
    public int saveOrUpdate(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为空");
        }

        Serializable id = getId(entity);
        if (id != null) {
            // 检查记录是否存在
            T existing = selectById(id);
            if (existing != null) {
                return updateById(entity);
            }
        }
        return insert(entity);
    }

    /**
     * 检查记录是否存在
     *
     * @param id 主键
     * @return true:存在 false:不存在
     */
    protected boolean existsById(Serializable id) {
        if (id == null) {
            return false;
        }
        return selectById(id) != null;
    }

    /**
     * 检查字段值是否唯一
     *
     * @param fieldName 字段名
     * @param fieldValue 字段值
     * @param excludeId 排除的ID
     * @return true:唯一 false:不唯一
     */
    protected boolean isFieldUnique(String fieldName, Object fieldValue, Serializable excludeId) {
        // 这里需要子类根据具体业务实现
        // 可以通过反射或者自定义查询方法实现
        log.warn("isFieldUnique方法需要子类重写实现具体业务逻辑");
        return true;
    }

    /**
     * 设置创建信息
     *
     * @param entity 实体对象
     * @param createBy 创建者
     */
    protected void setCreateInfo(T entity, String createBy) {
        if (entity != null) {
            entity.setCreateBy(createBy);
            entity.setCreateTime(java.time.LocalDateTime.now());
        }
    }

    /**
     * 设置更新信息
     *
     * @param entity 实体对象
     * @param updateBy 更新者
     */
    protected void setUpdateInfo(T entity, String updateBy) {
        if (entity != null) {
            entity.setUpdateBy(updateBy);
            entity.setUpdateTime(java.time.LocalDateTime.now());
        }
    }
}