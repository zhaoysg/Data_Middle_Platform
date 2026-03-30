from __future__ import annotations

import argparse
from pathlib import Path

import pymysql
import sqlparse


ROOT = Path(__file__).resolve().parents[1]
SQL_FILES = [
    ROOT / "script" / "sql" / "ry_vue_5.X.sql",
    ROOT / "script" / "sql" / "ry_job.sql",
    ROOT / "script" / "sql" / "ry_workflow.sql",
]


def execute_sql_file(connection: pymysql.Connection, sql_file: Path) -> int:
    sql_text = sql_file.read_text(encoding="utf-8", errors="ignore")
    statements = sqlparse.split(sql_text)
    executed = 0

    with connection.cursor() as cursor:
        for statement in statements:
            cleaned = sqlparse.format(statement, strip_comments=True).strip()
            if not cleaned:
                continue
            cursor.execute(statement)
            executed += 1

    return executed


def database_exists(connection: pymysql.Connection, database: str) -> bool:
    with connection.cursor() as cursor:
        cursor.execute("SHOW DATABASES LIKE %s", (database,))
        return cursor.fetchone() is not None


def create_database(connection: pymysql.Connection, database: str) -> None:
    with connection.cursor() as cursor:
        cursor.execute(
            f"CREATE DATABASE `{database}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
        )


def drop_database(connection: pymysql.Connection, database: str) -> None:
    with connection.cursor() as cursor:
        cursor.execute(f"DROP DATABASE IF EXISTS `{database}`")


def main() -> None:
    parser = argparse.ArgumentParser(description="Initialize a new remote MySQL database for this project.")
    parser.add_argument("--host", required=True)
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--database", required=True)
    parser.add_argument("--drop-if-exists", action="store_true")
    args = parser.parse_args()

    base_connection = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        charset="utf8mb4",
        autocommit=True,
        connect_timeout=15,
        read_timeout=600,
        write_timeout=600,
    )

    try:
        exists = database_exists(base_connection, args.database)
        if exists and args.drop_if_exists:
            print(f"Dropping existing database: {args.database}")
            drop_database(base_connection, args.database)
            exists = False

        if exists:
            raise SystemExit(f"Database already exists: {args.database}")

        print(f"Creating database: {args.database}")
        create_database(base_connection, args.database)
    finally:
        base_connection.close()

    project_connection = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
        autocommit=True,
        connect_timeout=15,
        read_timeout=600,
        write_timeout=600,
    )

    total = 0
    try:
        for sql_file in SQL_FILES:
            executed = execute_sql_file(project_connection, sql_file)
            total += executed
            print(f"{sql_file.name}: executed {executed} statements")
    finally:
        project_connection.close()

    print(f"Done. Database `{args.database}` is ready with {total} statements executed.")


if __name__ == "__main__":
    main()
