FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json ./

# proxy-ca-bundle.crt.build-only / BUILD_HTTPS_PROXY are optional and only
# used, when present, to let `npm ci` reach the registry when building
# inside a sandboxed dev environment with an intercepting egress proxy.
# Neither exists for a normal build (this repo's .gitignore keeps the CA
# file out entirely), and neither persists past this single RUN layer into
# the runtime environment.
COPY proxy-ca-bundle.crt.build-only* /tmp/
ARG BUILD_HTTPS_PROXY
RUN if [ -f /tmp/proxy-ca-bundle.crt.build-only ]; then \
      export NODE_EXTRA_CA_CERTS=/tmp/proxy-ca-bundle.crt.build-only; \
      export npm_config_cafile=/tmp/proxy-ca-bundle.crt.build-only; \
      export HTTPS_PROXY="$BUILD_HTTPS_PROXY"; \
      export npm_config_https_proxy="$BUILD_HTTPS_PROXY"; \
    fi; \
    npm ci --omit=dev; \
    rm -f /tmp/proxy-ca-bundle.crt.build-only

COPY . .
RUN rm -f proxy-ca-bundle.crt.build-only

ENV NODE_ENV=production
EXPOSE 3000

CMD ["node", "index.js"]
