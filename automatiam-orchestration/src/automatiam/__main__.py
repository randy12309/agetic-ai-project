"""Entry point: ``python -m automatiam``.

Uvicorn is configured here rather than on a command line so that the settings that matter - no
access log, no server header, the proxy headers the mesh sets - cannot be lost by someone starting
it with a shorter command.
"""

from __future__ import annotations

import uvicorn

from automatiam.api.app import create_app
from automatiam.core.config import get_settings


def main() -> None:
    settings = get_settings()
    uvicorn.run(
        create_app(settings),
        host="0.0.0.0",  # noqa: S104 - a container port, published only to the pod network
        port=settings.port,
        log_config=None,  # configure_logging has already set the handlers up for Splunk
        # Uvicorn's access log duplicates every request as unstructured text alongside this
        # service's own structured log line, and includes the full query string.
        access_log=False,
        server_header=False,
        date_header=False,
        # Honour X-Forwarded-* from the mesh sidecar only; this process is never internet-facing.
        proxy_headers=True,
        forwarded_allow_ips="127.0.0.1",
        timeout_keep_alive=int(settings.stream_idle_timeout_seconds),
    )


if __name__ == "__main__":
    main()
