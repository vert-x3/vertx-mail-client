require 'vertx/vertx'
require 'vertx/util/utils.rb'
# Generated from io.vertx.ext.mail.MailClient
module VertxMail
  class MailClient
    # @private
    # @param j_del [::VertxMail::MailClient] the java delegate
    def initialize(j_del)
      @j_del = j_del
    end
    # @private
    # @return [::VertxMail::MailClient] the underlying java delegate
    def j_del
      @j_del
    end
    # @param [::Vertx::Vertx] vertx 
    # @param [Hash] config 
    # @return [::VertxMail::MailClient]
    def self.create_non_shared(vertx=nil,config=nil)
      if vertx.class.method_defined?(:j_del) && config.class == Hash && !block_given?
        return ::Vertx::Util::Utils.safe_create(Java::IoVertxExtMail::MailClient.java_method(:createNonShared, [Java::IoVertxCore::Vertx.java_class,Java::IoVertxExtMail::MailConfig.java_class]).call(vertx.j_del,Java::IoVertxExtMail::MailConfig.new(::Vertx::Util::Utils.to_json_object(config))),::VertxMail::MailClient)
      end
      raise ArgumentError, "Invalid arguments when calling create_non_shared(vertx,config)"
    end
    # @param [::Vertx::Vertx] vertx 
    # @param [Hash] config 
    # @param [String] poolName 
    # @return [::VertxMail::MailClient]
    def self.create_shared(vertx=nil,config=nil,poolName=nil)
      if vertx.class.method_defined?(:j_del) && config.class == Hash && !block_given? && poolName == nil
        return ::Vertx::Util::Utils.safe_create(Java::IoVertxExtMail::MailClient.java_method(:createShared, [Java::IoVertxCore::Vertx.java_class,Java::IoVertxExtMail::MailConfig.java_class]).call(vertx.j_del,Java::IoVertxExtMail::MailConfig.new(::Vertx::Util::Utils.to_json_object(config))),::VertxMail::MailClient)
      elsif vertx.class.method_defined?(:j_del) && config.class == Hash && poolName.class == String && !block_given?
        return ::Vertx::Util::Utils.safe_create(Java::IoVertxExtMail::MailClient.java_method(:createShared, [Java::IoVertxCore::Vertx.java_class,Java::IoVertxExtMail::MailConfig.java_class,Java::java.lang.String.java_class]).call(vertx.j_del,Java::IoVertxExtMail::MailConfig.new(::Vertx::Util::Utils.to_json_object(config)),poolName),::VertxMail::MailClient)
      end
      raise ArgumentError, "Invalid arguments when calling create_shared(vertx,config,poolName)"
    end
    # @param [Hash] arg0 
    # @yield 
    # @return [self]
    def send_mail(arg0=nil)
      if arg0.class == Hash && block_given?
        @j_del.java_method(:sendMail, [Java::IoVertxExtMail::MailMessage.java_class,Java::IoVertxCore::Handler.java_class]).call(Java::IoVertxExtMail::MailMessage.new(::Vertx::Util::Utils.to_json_object(arg0)),(Proc.new { |ar| yield(ar.failed ? ar.cause : nil, ar.succeeded ? ar.result != nil ? JSON.parse(ar.result.toJson.encode) : nil : nil) }))
        return self
      end
      raise ArgumentError, "Invalid arguments when calling send_mail(arg0)"
    end
    # @return [void]
    def close
      if !block_given?
        return @j_del.java_method(:close, []).call()
      end
      raise ArgumentError, "Invalid arguments when calling close()"
    end
  end
end
