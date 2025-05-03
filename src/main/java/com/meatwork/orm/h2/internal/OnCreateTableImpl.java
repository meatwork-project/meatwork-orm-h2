package com.meatwork.orm.h2.internal;


import com.meatwork.core.api.di.Service;
import com.meatwork.orm.api.AbstractMeatEntity;
import com.meatwork.orm.api.DecimalProperty;
import com.meatwork.orm.api.Entity;
import com.meatwork.orm.api.EntityRefProperty;
import com.meatwork.orm.api.MetaProperty;
import com.meatwork.orm.api.OnCreateTable;
import com.meatwork.orm.api.OrmQueryException;
import com.meatwork.orm.api.PropertyType;
import com.meatwork.orm.api.TableBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Copyright (c) 2025 Taliro.
 * All rights reserved.
 */
@Service
public class OnCreateTableImpl implements OnCreateTable {
	@Override
	public String onCreate(Set<Entity> entitySet) throws OrmQueryException {
		var sortedEntities = sortEntitiesByDependencies(entitySet);
		var strQueryList = new ArrayList<String>();

		for (var entity : sortedEntities) {
			var tableBuilder = new TableBuilder(((AbstractMeatEntity) entity).getTableName());
			var metaProperties = ((AbstractMeatEntity) entity).getMetaProperties();
			for (var metaProperty : metaProperties) {
				processPrimaryKey(
						tableBuilder,
						metaProperty
				);
				processTypeConverter(
						tableBuilder,
						metaProperty
				);
			}
			strQueryList.add(tableBuilder.build());
		}

		return String.join(
				"\n",
				strQueryList
		);
	}

	private void processPrimaryKey(TableBuilder tableBuilder,
	                               MetaProperty metaProperty) {
		if (metaProperty.primaryKey()) {
			tableBuilder.primaryKey(
					metaProperty.fieldName()
			);
		}
	}

	private List<Entity> sortEntitiesByDependencies(Set<Entity> entitySet) {
		var dependencyGraph = new HashMap<Entity, List<Entity>>();

		for (var entity : entitySet) {
			var dependencies = Stream
					.of(((AbstractMeatEntity) entity)
							    .getMetaProperties())
					.filter(metaProperty -> metaProperty.type() == PropertyType.ENTITY_REF)
					.map(metaProperty ->
							     ((EntityRefProperty) Stream
									     .of(metaProperty.extraMetraProperties())
									     .filter(it -> it instanceof EntityRefProperty)
									     .findFirst()
									     .orElseThrow())
					)
					.map(entityRefProperty -> entitySet
							.stream()
							.filter(it -> ((AbstractMeatEntity) it)
									.getTableName()
									.equals(entityRefProperty.tableName()))
							.findFirst()
							.orElseThrow()
					)
					.collect(Collectors.toList());
			dependencyGraph.put(
					entity,
					dependencies
			);
		}

		var sortedEntities = new ArrayList<Entity>();
		var visited = new HashSet<Entity>();

		for (var entity : entitySet) {
			visitEntity(
					entity,
					dependencyGraph,
					visited,
					sortedEntities
			);
		}

		return sortedEntities;
	}

	private void visitEntity(Entity entity,
	                         Map<Entity, List<Entity>> graph,
	                         Set<Entity> visited,
	                         List<Entity> sorted) {
		if (visited.contains(entity)) {
			return;
		}
		visited.add(entity);

		for (var dependency : graph.getOrDefault(
				entity,
				List.of()
		)) {
			visitEntity(
					dependency,
					graph,
					visited,
					sorted
			);
		}

		sorted.add(entity);
	}

	private void processTypeConverter(TableBuilder tableBuilder,
	                                  MetaProperty metaProperty) throws OrmQueryException {
		switch (metaProperty.type()) {
			case LONG -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"BIGINT",
					!metaProperty.nullable()
			);
			case ENTITY_REF -> {
				EntityRefProperty extraMetraProperty = Stream
						.of(metaProperty.extraMetraProperties())
						.filter(it -> EntityRefProperty.class.equals(it.getClass()))
						.map(EntityRefProperty.class::cast)
						.findFirst()
						.orElse(null);
				if (extraMetraProperty != null) {
					throw new OrmQueryException("Cannot found EntityRefProperty for property %s".formatted(metaProperty.fieldName()));
				}
				tableBuilder.foreignKey(
						metaProperty.fieldName(),
						extraMetraProperty.tableName(),
						extraMetraProperty.columnNameId()
				);
				processTypeConverter(
						tableBuilder,
						new MetaProperty(
								metaProperty.fieldName(),
								extraMetraProperty.typeId(),
								false,
								false,
								false
						)
				);
			}
			case STRING -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"VARCHAR(255)",
					!metaProperty.nullable()
			);
			case INTEGER -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"INTEGER",
					!metaProperty.nullable()
			);
			case BOOLEAN -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"BOOLEAN",
					!metaProperty.nullable()
			);
			case LOCALDATE -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"DATE",
					!metaProperty.nullable()
			);
			case LOCALTIME -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"TIMESTAMP(6)",
					!metaProperty.nullable()
			);
			case LOCALDATETIME -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"TIMESTAMP",
					!metaProperty.nullable()
			);
			case BIGDECIMAL -> {
				DecimalProperty decimalProperty = Stream
						.of(metaProperty.extraMetraProperties())
						.filter(it -> DecimalProperty.class.equals(it.getClass()))
						.map(DecimalProperty.class::cast)
						.findFirst()
						.orElse(new DecimalProperty(
								5,
								2
						));
				tableBuilder.addColumn(
						metaProperty.fieldName(),
						"DECIMAL(%s,%s)".formatted(
								decimalProperty.precision(),
								decimalProperty.scale()
						),
						!metaProperty.nullable()
				);
			}
		}
	}
}
