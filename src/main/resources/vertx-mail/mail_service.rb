require 'vertx/util/utils.rb'
# Generated from io.vertx.ext.mail.MailService
module VertxMail
  #  smtp mail service for vert.x
  #  
  #  this Interface provides the methods to be used by the application program and is used to
  #  generate the service in other languages
  class MailService
    # @private
    # @param j_del [::VertxMail::MailService] the java delegate
    def initialize(j_del)
      @j_del = j_del
    end
    # @private
    # @return [::VertxMail::MailService] the underlying java delegate
    def j_del
      @j_del
    end
    #  create an instance of MailService that is running in the local JVM
    # @param [::Vertx::Vertx] vertx the Vertx instance the operation will be run in
    # @param [Hash] config MailConfig configuration to be used for sending mails
    # @return [::VertxMail::MailService] MailService instance that can then be used to send multiple mails
    def self.create(vertx=nil,config=nil)
      if vertx.class.method_defined?(:j_del) && config.class == Hash && !block_given?
        return ::VertxMail::MailService.new(Java::IoVertxExtMail::MailService.java_method(:create, [Java::IoVertxCore::Vertx.java_class,Java::IoVertxExtMail::MailConfig.java_class]).call(vertx.j_del,Java::IoVertxExtMail::MailConfig.new(::Vertx::Util::Utils.to_json_object(config))))
      end
      raise ArgumentError, "Invalid arguments when calling create(vertx,config)"
    end
    #  create an instance of  MailService that calls the mail service via the event bus running somewhere else
    # @param [::Vertx::Vertx] vertx the Vertx instance the operation will be run in
    # @param [String] address the eb address of the mail service running somewhere, default is "vertx.mail"
    # @return [::VertxMail::MailService] MailService instance that can then be used to send multiple mails
    def self.create_event_bus_proxy(vertx=nil,address=nil)
      if vertx.class.method_defined?(:j_del) && address.class == String && !block_given?
        return ::VertxMail::MailService.new(Java::IoVertxExtMail::MailService.java_method(:createEventBusProxy, [Java::IoVertxCore::Vertx.java_class,Java::java.lang.String.java_class]).call(vertx.j_del,address))
      end
      raise ArgumentError, "Invalid arguments when calling create_event_bus_proxy(vertx,address)"
    end
    #  send a single mail via MailService
    # @param [Hash] email MailMessage object containing the mail text, from/to, attachments etc
    # @yield will be called when the operation is finished or it fails (may be null to ignore the result) the result JsonObject currently only contains {@code {"result":"success"}}
    # @return [self]
    def send_mail(email=nil)
      if email.class == Hash && block_given?
        @j_del.java_method(:sendMail, [Java::IoVertxExtMail::MailMessage.java_class,Java::IoVertxCore::Handler.java_class]).call(Java::IoVertxExtMail::MailMessage.new(::Vertx::Util::Utils.to_json_object(email)),(Proc.new { |ar| yield(ar.failed ? ar.cause : nil, ar.succeeded ? ar.result != nil ? JSON.parse(ar.result.encode) : nil : nil) }))
        return self
      end
      raise ArgumentError, "Invalid arguments when calling send_mail(email)"
    end
    #  start the MailServer instance if it is running locally (this operation is currently a no-op)
    # @return [void]
    def start
      if !block_given?
        return @j_del.java_method(:start, []).call()
      end
      raise ArgumentError, "Invalid arguments when calling start()"
    end
    #  stop the MailServer instance if it is running locally
    #  <p>
    #  this operation shuts down the connection pool, doesn't wait for completion of the close operations
    #  when the mail service is running on the event bus, this operation has no effect
    # @return [void]
    def stop
      if !block_given?
        return @j_del.java_method(:stop, []).call()
      end
      raise ArgumentError, "Invalid arguments when calling stop()"
    end
  end
end
