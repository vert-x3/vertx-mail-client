require 'vertx-mail/mail_client'
require 'vertx/util/utils.rb'
# Generated from io.vertx.ext.mail.MailService
module VertxMail
  #  @author <a href="http://tfox.org">Tim Fox</a>
  class MailService < ::VertxMail::MailClient
    # @private
    # @param j_del [::VertxMail::MailService] the java delegate
    def initialize(j_del)
      super(j_del)
      @j_del = j_del
    end
    # @private
    # @return [::VertxMail::MailService] the underlying java delegate
    def j_del
      @j_del
    end
    # @param [Hash] email
    # @yield 
    # @return [self]
    def send_mail(email=nil)
      if email.class == Hash && block_given?
        @j_del.java_method(:sendMail, [Java::IoVertxExtMail::MailMessage.java_class,Java::IoVertxCore::Handler.java_class]).call(Java::IoVertxExtMail::MailMessage.new(::Vertx::Util::Utils.to_json_object(email)),(Proc.new { |ar| yield(ar.failed ? ar.cause : nil, ar.succeeded ? ar.result != nil ? JSON.parse(ar.result.encode) : nil : nil) }))
        return self
      end
      raise ArgumentError, "Invalid arguments when calling send_mail(email)"
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
