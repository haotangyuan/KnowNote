FROM node:18-alpine
WORKDIR /workspace
COPY template/ /workspace-template/
RUN cd /workspace-template && npm install
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
EXPOSE 3000 4000
ENTRYPOINT ["/entrypoint.sh"]
