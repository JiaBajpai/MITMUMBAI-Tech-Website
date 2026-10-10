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

Flyway is the only schema-management mechanism and runs on application startup
when enabled. Both `dev` and `prod` use the Maven-generated
`db/migration-production` resource set, which excludes V9 and V10. This keeps
normal startup free of historical demo accounts and business records. The
explicit `demo` profile uses the full historical `db/migration` location and
is only for a disposable database. Do not change V1–V10 after they have been
applied.

The complete historical chain is also available through the explicit `demo`
profile for a disposable local database:

```sh
./mvnw -Dspring-boot.run.profiles=demo spring-boot:run
```

Use that profile only against a disposable local database. It can insert V9 or
V10 if either migration is pending. Never use it on a database with genuine
or unknown data. A database that already records V9/V10 will not validate
against the normal dev/prod bundle because those versions are intentionally
unresolved there. Do not baseline, repair, or delete records to silence a
validation error. Resolve such histories through an approved compatibility
plan; do not start the application until then.

Automatic baselining is disabled. Never baseline an existing database without
a separately reviewed migration-history procedure.

## Production Configuration

In production, Flyway is the **only** mechanism for schema changes.
Hibernate `ddl-auto` is set to `validate` to prevent automatic schema modifications.
