# Flyway Database Migrations

This directory contains Flyway database migration scripts for the MIT TECH KERNEL backend.

## Migration Naming Convention

All migration files must follow the Flyway naming convention:

```
V<version>__<description>.sql
```

Examples:
- `V1__initial_schema.sql`
- `V2__add_users_table.sql`
- `V3__add_indexes.sql`

## Rules

1. **Never modify existing migrations** - Once a migration has been applied to any environment, it must not be changed.
2. **Always create new migrations** - For any schema changes, create a new migration file with the next version number.
3. **Use descriptive names** - The description should clearly indicate what the migration does.
4. **Version numbering** - Use sequential integers (V1, V2, V3, ...).
5. **SQL only** - Use `.sql` files for migrations. Java-based migrations are not used in this project.
6. **Test migrations** - Always test migrations locally before committing.

## Applying Migrations

Migrations run automatically on application startup when `spring.flyway.enabled=true` (default).

To run migrations manually:
```bash
mvn flyway:migrate
```

To check migration status:
```bash
mvn flyway:info
```

To baseline an existing database:
```bash
mvn flyway:baseline -Dflyway.baselineVersion=0
```

## Production Configuration

In production, Flyway is the **only** mechanism for schema changes.
Hibernate `ddl-auto` is set to `validate` to prevent automatic schema modifications.