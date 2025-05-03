package com.meatork.orm.h2.internal;


import com.meatwork.core.api.di.Service;
import com.meatwork.orm.api.AbstractMeatEntity;
import com.meatwork.orm.api.Entity;
import com.meatwork.orm.api.MetaProperty;
import com.meatwork.orm.api.OnCreateTable;
import com.meatwork.orm.api.TableBuilder;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Copyright (c) 2025 Taliro.
 * All rights reserved.
 */
@Service
public class OnCreateTableImpl implements OnCreateTable {
	@Override
	public String onCreate(Set<Entity> entitySet) {
		var strQueryList = new ArrayList<String>();
		for (var entity : entitySet) {
			var tableBuilder = new TableBuilder(((AbstractMeatEntity)entity).getTableName());
			var metaProperties = ((AbstractMeatEntity)entity).getMetaProperties();
			for (var metaProperty : metaProperties) {
				processTypeConverter(
						tableBuilder,
						metaProperty
				);
			}
			strQueryList.add(tableBuilder.build());
		}
		return strQueryList.stream().collect(Collectors.joining("\n"));
	}

	private void processTypeConverter(TableBuilder tableBuilder,
	                                  MetaProperty metaProperty) {
		switch (metaProperty.type()) {
			case LONG, ENTITY_REF -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"BIGINT",
					!metaProperty.nullable()
			);
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
					"TIME",
					!metaProperty.nullable()
			);
			case LOCALDATETIME -> tableBuilder.addColumn(
					metaProperty.fieldName(),
					"TIMESTAMP",
					!metaProperty.nullable()
			);
		}
	}


}
